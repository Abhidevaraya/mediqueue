package com.mediqueue.appointment.service;

import com.mediqueue.appointment.dto.AppointmentBookingRequest;
import com.mediqueue.appointment.dto.AppointmentCancelRequest;
import com.mediqueue.appointment.dto.AppointmentRescheduleRequest;
import com.mediqueue.appointment.dto.AppointmentResponse;
import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.hook.AppointmentBookingHook;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.common.PageResponse;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.schedule.entity.SlotInventory;
import com.mediqueue.schedule.repository.SlotInventoryRepository;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.schedule.service.SlotInventoryService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppointmentRepository appointmentRepository;
    private final SlotInventoryRepository slotInventoryRepository;
    private final SlotInventoryService slotInventoryService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AvailabilityService availabilityService;
    private final AppointmentBookingHook appointmentBookingHook;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final Set<AppointmentStatus> ACTIVE_STATUSES = Set.of(
            AppointmentStatus.BOOKED,
            AppointmentStatus.CHECKED_IN,
            AppointmentStatus.IN_PROGRESS
    );

    @Transactional
    public AppointmentResponse bookAppointment(AuthenticatedUser principal, AppointmentBookingRequest request) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if (doctor.getVerificationStatus() != DoctorVerificationStatus.VERIFIED ||
                doctor.getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Doctor is currently inactive or not verified for appointments");
        }

        LocalDateTime nowHospital = availabilityService.nowInHospitalTime().toLocalDateTime();
        if (request.getStartDateTime().isBefore(nowHospital)) {
            throw new ConflictException("Cannot book appointment slots in the past");
        }

        // Validate that requested start time forms an available schedule slot
        AvailabilityService.SlotTemplate slotTemplate =
                availabilityService.validateAndResolveSlotTemplate(doctor, request.getStartDateTime());

        // Check active overlapping appointment for this patient
        boolean patientConflict = appointmentRepository.hasConflictingActiveAppointment(
                patient.getId(),
                slotTemplate.startDateTime(),
                slotTemplate.endDateTime(),
                ACTIVE_STATUSES,
                null
        );
        if (patientConflict) {
            throw new ConflictException("You already have an active appointment scheduled during this time interval");
        }

        // Concurrency Control: Ensure inventory row exists in isolated transaction, then acquire pessimistic write lock
        slotInventoryService.ensureSlotInventoryExists(doctor, slotTemplate.startDateTime(), slotTemplate.endDateTime(), slotTemplate.capacity());

        SlotInventory inventory = slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(doctor.getId(), slotTemplate.startDateTime())
                .orElseThrow(() -> new IllegalStateException("Failed to acquire slot inventory record with lock"));

        if (inventory.getBookedCount() >= inventory.getTotalCapacity()) {
            throw new ConflictException("The selected appointment slot has reached maximum booking capacity");
        }

        inventory.setBookedCount(inventory.getBookedCount() + 1);
        slotInventoryRepository.save(inventory);

        // Generate non-guessable appointment reference
        String appointmentRef = generateUniqueReference(slotTemplate.startDateTime().toLocalDate());

        Appointment appointment = Appointment.builder()
                .appointmentReference(appointmentRef)
                .patient(patient)
                .doctor(doctor)
                .department(doctor.getDepartment())
                .startDateTime(slotTemplate.startDateTime())
                .endDateTime(slotTemplate.endDateTime())
                .slotDurationMinutes(slotTemplate.durationMinutes())
                .status(AppointmentStatus.BOOKED)
                .reason(request.getReason() != null ? request.getReason().trim() : null)
                .bookingTimestamp(Instant.now())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Phase 3 integration trigger
        appointmentBookingHook.onAppointmentBooked(savedAppointment);

        log.info("Appointment booked successfully. Reference: {}", appointmentRef);
        return AppointmentResponse.from(savedAppointment);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(AuthenticatedUser principal, Long appointmentId, AppointmentCancelRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        validateAppointmentCancellationOwnership(principal, appointment);

        if (appointment.getStatus() != AppointmentStatus.BOOKED && appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw new ConflictException("Appointment in status '" + appointment.getStatus() + "' cannot be cancelled");
        }

        // Release slot capacity atomically via SlotInventory lock
        Optional<SlotInventory> invOptional = slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(
                appointment.getDoctor().getId(),
                appointment.getStartDateTime()
        );
        invOptional.ifPresent(inv -> {
            inv.setBookedCount(Math.max(0, inv.getBookedCount() - 1));
            slotInventoryRepository.save(inv);
        });

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationTimestamp(Instant.now());
        appointment.setCancellationReason(request != null && request.getReason() != null ? request.getReason().trim() : null);

        Appointment updated = appointmentRepository.save(appointment);
        eventPublisher.publishEvent(new com.mediqueue.appointment.event.AppointmentCancelledEvent(this, updated, updated.getCancellationReason()));
        log.info("Appointment {} cancelled successfully", appointment.getAppointmentReference());
        return AppointmentResponse.from(updated);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(AuthenticatedUser principal, Long appointmentId, AppointmentRescheduleRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        validateAppointmentReschedulingOwnership(principal, appointment);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ConflictException("Only appointments in BOOKED status can be rescheduled. Current status: " + appointment.getStatus());
        }

        if (request.getNewStartDateTime().equals(appointment.getStartDateTime())) {
            throw new BusinessException("New start time must be different from current appointment time");
        }

        LocalDateTime nowHospital = availabilityService.nowInHospitalTime().toLocalDateTime();
        if (request.getNewStartDateTime().isBefore(nowHospital)) {
            throw new ConflictException("Cannot reschedule to a slot in the past");
        }

        Doctor doctor = appointment.getDoctor();
        AvailabilityService.SlotTemplate newTemplate =
                availabilityService.validateAndResolveSlotTemplate(doctor, request.getNewStartDateTime());

        // Check patient conflict on new slot excluding this appointment
        boolean patientConflict = appointmentRepository.hasConflictingActiveAppointment(
                appointment.getPatient().getId(),
                newTemplate.startDateTime(),
                newTemplate.endDateTime(),
                ACTIVE_STATUSES,
                appointment.getId()
        );
        if (patientConflict) {
            throw new ConflictException("You already have another active appointment overlapping with the new time slot");
        }

        // Deterministic Lock Ordering by slotStartTime to prevent deadlocks between competing reschedules
        LocalDateTime oldStart = appointment.getStartDateTime();
        LocalDateTime newStart = newTemplate.startDateTime();

        slotInventoryService.ensureSlotInventoryExists(doctor, oldStart, appointment.getEndDateTime(), 1);
        slotInventoryService.ensureSlotInventoryExists(doctor, newStart, newTemplate.endDateTime(), newTemplate.capacity());

        SlotInventory firstLock;
        SlotInventory secondLock;

        if (oldStart.isBefore(newStart)) {
            firstLock = slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(doctor.getId(), oldStart).orElseThrow();
            secondLock = slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(doctor.getId(), newStart).orElseThrow();
        } else {
            secondLock = slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(doctor.getId(), newStart).orElseThrow();
            firstLock = slotInventoryRepository.findByDoctorIdAndSlotStartTimeWithLock(doctor.getId(), oldStart).orElseThrow();
        }

        SlotInventory oldInventory = oldStart.equals(firstLock.getSlotStartTime()) ? firstLock : secondLock;
        SlotInventory newInventory = newStart.equals(firstLock.getSlotStartTime()) ? firstLock : secondLock;

        if (newInventory.getBookedCount() >= newInventory.getTotalCapacity()) {
            throw new ConflictException("The target appointment slot is fully booked");
        }

        // Release old slot & Reserve new slot atomically
        oldInventory.setBookedCount(Math.max(0, oldInventory.getBookedCount() - 1));
        newInventory.setBookedCount(newInventory.getBookedCount() + 1);
        slotInventoryRepository.save(oldInventory);
        slotInventoryRepository.save(newInventory);

        String previousRef = appointment.getAppointmentReference();
        appointment.setRescheduledFromReference(previousRef);
        appointment.setRescheduledTimestamp(Instant.now());
        appointment.setStartDateTime(newTemplate.startDateTime());
        appointment.setEndDateTime(newTemplate.endDateTime());
        appointment.setSlotDurationMinutes(newTemplate.durationMinutes());

        Appointment updated = appointmentRepository.save(appointment);
        log.info("Appointment {} rescheduled to {}", previousRef, newTemplate.startDateTime());
        return AppointmentResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getMyAppointments(Long userId, AppointmentStatus status, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        Page<Appointment> page = appointmentRepository.findByPatientIdAndStatus(patient.getId(), status, pageable);
        return PageResponse.of(page, page.getContent().stream().map(AppointmentResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(AuthenticatedUser principal, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        validateAppointmentReadAccess(principal, appointment);
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorTodayAppointments(AuthenticatedUser principal) {
        Doctor doctor = resolveDoctorFromPrincipal(principal);
        LocalDate today = availabilityService.nowInHospitalTime().toLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<Appointment> list = appointmentRepository.findDoctorDailyAppointments(doctor.getId(), startOfDay, endOfDay);
        return list.stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> getAdminAppointments(Long doctorId, Long patientId, Long departmentId,
                                                                 AppointmentStatus status, LocalDateTime startFrom,
                                                                 LocalDateTime startTo, Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findAdminAppointments(
                doctorId, patientId, departmentId, status, startFrom, startTo, pageable
        );
        return PageResponse.of(page, page.getContent().stream().map(AppointmentResponse::from).toList());
    }

    private String generateUniqueReference(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            String candidate = "APT-" + datePrefix + "-" + sb;
            if (appointmentRepository.findByAppointmentReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate unique appointment reference");
    }

    private Doctor resolveDoctorFromPrincipal(AuthenticatedUser principal) {
        if (principal.getRole() == UserRole.DOCTOR) {
            return doctorRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for user ID: " + principal.getId()));
        }
        throw new AccessDeniedException("Only doctors can access doctor daily appointments");
    }

    private void validateAppointmentReadAccess(AuthenticatedUser principal, Appointment appointment) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (appointment.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            if (appointment.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: you do not have permission to view this appointment");
    }

    private void validateAppointmentCancellationOwnership(AuthenticatedUser principal, Appointment appointment) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (appointment.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            if (appointment.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: you do not have permission to cancel this appointment");
    }

    private void validateAppointmentReschedulingOwnership(AuthenticatedUser principal, Appointment appointment) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (appointment.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: only the patient owner or an administrator can reschedule this appointment");
    }
}
