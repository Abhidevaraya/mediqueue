package com.mediqueue.followup.service;

import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.followup.dto.FollowUpCreateRequest;
import com.mediqueue.followup.dto.FollowUpResponse;
import com.mediqueue.followup.entity.FollowUp;
import com.mediqueue.followup.enums.FollowUpReminderStatus;
import com.mediqueue.followup.enums.FollowUpStatus;
import com.mediqueue.followup.repository.FollowUpRepository;
import com.mediqueue.notification.enums.NotificationType;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
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
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowUpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final FollowUpRepository followUpRepository;
    private final ConsultationRecordRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final ClinicalAuditService auditService;

    @Transactional
    public FollowUpResponse createFollowUp(AuthenticatedUser principal, FollowUpCreateRequest request) {
        ConsultationRecord consultation = consultationRepository.findById(request.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + request.getConsultationId()));

        Doctor doctor = validateAndResolveDoctor(principal, consultation.getDoctor().getId());

        if (request.getRecommendedDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Follow-up date cannot be in the past");
        }

        FollowUp followUp = FollowUp.builder()
                .followUpReference(generateReference(LocalDate.now()))
                .consultation(consultation)
                .appointment(consultation.getAppointment())
                .patient(consultation.getPatient())
                .doctor(doctor)
                .recommendedDate(request.getRecommendedDate())
                .instructions(request.getInstructions())
                .status(FollowUpStatus.SCHEDULED)
                .reminderStatus(FollowUpReminderStatus.SENT)
                .reminderSentAt(Instant.now())
                .build();

        FollowUp saved = followUpRepository.save(followUp);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "FOLLOW_UP_CREATED",
                "FOLLOW_UP",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Created follow-up recommendation for " + saved.getRecommendedDate()
        );

        // Notify patient
        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.FOLLOW_UP_REMINDER,
                "Follow-Up Recommended",
                String.format("Dr. %s recommended a follow-up consultation on %s. Instructions: %s",
                        doctor.getUser().getFullName(),
                        saved.getRecommendedDate(),
                        saved.getInstructions() != null ? saved.getInstructions() : "Please book an appointment."),
                saved.getAppointment().getId(),
                null
        );

        log.info("Follow-up created: reference={} for patient={}", saved.getFollowUpReference(), saved.getPatient().getId());
        return FollowUpResponse.from(saved);
    }

    @Transactional
    public FollowUpResponse completeFollowUp(AuthenticatedUser principal, Long followUpId) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found with ID: " + followUpId));

        validateAndResolveDoctor(principal, followUp.getDoctor().getId());

        if (followUp.getStatus() != FollowUpStatus.SCHEDULED) {
            throw new ConflictException("Follow-up cannot be completed. Current status: " + followUp.getStatus());
        }

        followUp.setStatus(FollowUpStatus.COMPLETED);
        followUp.setCompletedAt(Instant.now());
        FollowUp saved = followUpRepository.save(followUp);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "FOLLOW_UP_COMPLETED",
                "FOLLOW_UP",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Marked follow-up completed " + saved.getFollowUpReference()
        );

        log.info("Follow-up completed: reference={}", saved.getFollowUpReference());
        return FollowUpResponse.from(saved);
    }

    @Transactional
    public FollowUpResponse cancelFollowUp(AuthenticatedUser principal, Long followUpId, String reason) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found with ID: " + followUpId));

        validateAndResolveDoctor(principal, followUp.getDoctor().getId());

        if (followUp.getStatus() != FollowUpStatus.SCHEDULED) {
            throw new ConflictException("Follow-up cannot be cancelled. Current status: " + followUp.getStatus());
        }

        followUp.setStatus(FollowUpStatus.CANCELLED);
        followUp.setCancellationReason(reason != null ? reason.trim() : "Cancelled by clinician");
        FollowUp saved = followUpRepository.save(followUp);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "FOLLOW_UP_CANCELLED",
                "FOLLOW_UP",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Cancelled follow-up " + saved.getFollowUpReference()
        );

        log.info("Follow-up cancelled: reference={}", saved.getFollowUpReference());
        return FollowUpResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public FollowUpResponse getFollowUpById(AuthenticatedUser principal, Long followUpId) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found with ID: " + followUpId));

        validateViewAccess(principal, followUp);
        return FollowUpResponse.from(followUp);
    }

    @Transactional(readOnly = true)
    public Page<FollowUpResponse> getPatientFollowUps(AuthenticatedUser principal, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

        return followUpRepository.findByPatientIdOrderByRecommendedDateAsc(patient.getId(), pageable)
                .map(FollowUpResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FollowUpResponse> getDoctorFollowUps(AuthenticatedUser principal, Pageable pageable) {
        Doctor doctor = resolveDoctorFromPrincipal(principal);
        return followUpRepository.findByDoctorIdOrderByRecommendedDateAsc(doctor.getId(), pageable)
                .map(FollowUpResponse::from);
    }

    private void validateViewAccess(AuthenticatedUser principal, FollowUp followUp) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }

        if (principal.getRole() == UserRole.PATIENT) {
            if (followUp.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }

        if (principal.getRole() == UserRole.DOCTOR) {
            if (followUp.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }

        throw new AccessDeniedException("Access denied: You do not have permission to view this follow-up recommendation");
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

        throw new AccessDeniedException("Access denied: You are not authorized to manage follow-ups for doctor ID " + doctorId);
    }

    private Doctor resolveDoctorFromPrincipal(AuthenticatedUser principal) {
        if (principal.getRole() == UserRole.ADMIN) {
            return doctorRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No doctors exist in the system"));
        }
        return doctorRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for authenticated user"));
    }

    private String generateReference(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
        }
        return "FLW-" + datePrefix + "-" + sb;
    }
}
