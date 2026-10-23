package com.mediqueue.consultation.service;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.dto.ConsultationResponse;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.enums.ConsultationStatus;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final ConsultationRecordRepository consultationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final ClinicalAuditService auditService;

    @Transactional
    public ConsultationResponse startConsultation(AuthenticatedUser principal, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        Doctor doctor = validateAndResolveDoctor(principal, appointment.getDoctor().getId());

        // Check for existing consultation for this appointment
        Optional<ConsultationRecord> existingOpt = consultationRepository.findByAppointmentId(appointmentId);
        if (existingOpt.isPresent()) {
            ConsultationRecord existing = existingOpt.get();
            if (existing.getStatus() == ConsultationStatus.IN_PROGRESS) {
                return ConsultationResponse.from(existing);
            }
            if (existing.getStatus() == ConsultationStatus.COMPLETED) {
                throw new ConflictException("Consultation for this appointment has already been completed");
            }
            if (existing.getStatus() == ConsultationStatus.CANCELLED) {
                throw new ConflictException("Consultation for this appointment was cancelled");
            }
        }

        // Check if doctor currently has another consultation in progress
        Optional<ConsultationRecord> doctorActive = consultationRepository.findFirstByDoctorIdAndStatus(
                doctor.getId(), ConsultationStatus.IN_PROGRESS
        );
        if (doctorActive.isPresent()) {
            throw new ConflictException("Another clinical consultation is already in progress: " + doctorActive.get().getConsultationReference());
        }

        Instant now = Instant.now();
        ConsultationRecord record = existingOpt.orElseGet(() -> ConsultationRecord.builder()
                .consultationReference(generateReference(appointment.getStartDateTime().toLocalDate()))
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(doctor)
                .build());

        record.setStatus(ConsultationStatus.IN_PROGRESS);
        record.setActualStartTimestamp(now);
        ConsultationRecord saved = consultationRepository.save(record);

        // Synchronize appointment
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(appointment);

        // Synchronize queue entry if exists
        queueEntryRepository.findByAppointmentId(appointmentId).ifPresent(qe -> {
            if (qe.getStatus() == QueueStatus.CALLED || qe.getStatus() == QueueStatus.WAITING) {
                qe.setStatus(QueueStatus.IN_PROGRESS);
                qe.setConsultationStartTimestamp(now);
                queueEntryRepository.save(qe);
            }
        });

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "CONSULTATION_STARTED",
                "CONSULTATION",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Started consultation for appointment " + appointment.getAppointmentReference()
        );

        log.info("Consultation started: reference={} for appointment={}", saved.getConsultationReference(), appointmentId);
        return ConsultationResponse.from(saved);
    }

    @Transactional
    public ConsultationResponse completeConsultation(AuthenticatedUser principal, Long consultationId) {
        ConsultationRecord record = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation record not found with ID: " + consultationId));

        validateAndResolveDoctor(principal, record.getDoctor().getId());

        if (record.getStatus() != ConsultationStatus.IN_PROGRESS) {
            throw new ConflictException("Consultation cannot be completed because current status is: " + record.getStatus());
        }

        Instant now = Instant.now();
        record.setStatus(ConsultationStatus.COMPLETED);
        record.setActualEndTimestamp(now);
        ConsultationRecord saved = consultationRepository.save(record);

        // Synchronize appointment
        Appointment appointment = record.getAppointment();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        // Synchronize queue entry if exists
        queueEntryRepository.findByAppointmentId(appointment.getId()).ifPresent(qe -> {
            if (qe.getStatus() == QueueStatus.IN_PROGRESS || qe.getStatus() == QueueStatus.CALLED) {
                qe.setStatus(QueueStatus.COMPLETED);
                qe.setConsultationEndTimestamp(now);
                queueEntryRepository.save(qe);
            }
        });

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "CONSULTATION_COMPLETED",
                "CONSULTATION",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Completed consultation " + saved.getConsultationReference()
        );

        log.info("Consultation completed: reference={}", saved.getConsultationReference());
        return ConsultationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(AuthenticatedUser principal, Long consultationId) {
        ConsultationRecord record = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation record not found with ID: " + consultationId));

        validateAccess(principal, record);
        return ConsultationResponse.from(record);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getDoctorConsultations(AuthenticatedUser principal, Pageable pageable) {
        Doctor doctor = resolveDoctorFromPrincipal(principal);
        return consultationRepository.findByDoctorIdOrderByCreatedAtDesc(doctor.getId(), pageable)
                .map(ConsultationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getPatientConsultations(AuthenticatedUser principal, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

        return consultationRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId(), pageable)
                .map(ConsultationResponse::from);
    }

    private Doctor validateAndResolveDoctor(AuthenticatedUser principal, Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (principal.getRole() == UserRole.ADMIN) {
            return doctor;
        }

        if (principal.getRole() == UserRole.DOCTOR) {
            if (doctor.getUser().getId().equals(principal.getId())) {
                return doctor;
            }
        }

        throw new AccessDeniedException("Access denied: You are not authorized to perform operations for doctor ID " + doctorId);
    }

    private Doctor resolveDoctorFromPrincipal(AuthenticatedUser principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            // Admin can access their own or the first doctor
            return doctorRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No doctors exist in the system"));
        }
        return doctorRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for authenticated user"));
    }

    private void validateAccess(AuthenticatedUser principal, ConsultationRecord record) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (record.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            if (record.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: You do not have permission to view this clinical consultation");
    }

    private String generateReference(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
            }
            String candidate = "CON-" + datePrefix + "-" + sb;
            if (consultationRepository.findByConsultationReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "CON-" + datePrefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
