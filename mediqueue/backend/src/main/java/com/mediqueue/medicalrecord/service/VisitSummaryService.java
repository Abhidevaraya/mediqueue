package com.mediqueue.medicalrecord.service;

import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.medicalrecord.dto.VisitSummaryCreateRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryResponse;
import com.mediqueue.medicalrecord.dto.VisitSummaryRevisionRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryUpdateRequest;
import com.mediqueue.medicalrecord.entity.VisitSummary;
import com.mediqueue.medicalrecord.entity.VisitSummaryRevision;
import com.mediqueue.medicalrecord.enums.VisitSummaryStatus;
import com.mediqueue.medicalrecord.repository.VisitSummaryRepository;
import com.mediqueue.medicalrecord.repository.VisitSummaryRevisionRepository;
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
public class VisitSummaryService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final VisitSummaryRepository summaryRepository;
    private final VisitSummaryRevisionRepository revisionRepository;
    private final ConsultationRecordRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final ClinicalAuditService auditService;

    @Transactional
    public VisitSummaryResponse createDraft(AuthenticatedUser principal, VisitSummaryCreateRequest request) {
        ConsultationRecord consultation = consultationRepository.findById(request.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + request.getConsultationId()));

        Doctor doctor = validateAndResolveDoctor(principal, consultation.getDoctor().getId());

        VisitSummary summary = VisitSummary.builder()
                .summaryReference(generateReference(LocalDate.now()))
                .consultation(consultation)
                .appointment(consultation.getAppointment())
                .patient(consultation.getPatient())
                .authoringDoctor(doctor)
                .chiefComplaint(request.getChiefComplaint())
                .diagnosis(request.getDiagnosis())
                .clinicalNotes(request.getClinicalNotes())
                .treatmentPlan(request.getTreatmentPlan())
                .status(VisitSummaryStatus.DRAFT)
                .versionNumber(1)
                .build();

        VisitSummary saved = summaryRepository.save(summary);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "VISIT_SUMMARY_CREATED",
                "VISIT_SUMMARY",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Created draft visit summary " + saved.getSummaryReference()
        );

        log.info("Created draft visit summary: reference={} by doctor={}", saved.getSummaryReference(), doctor.getId());
        return VisitSummaryResponse.from(saved);
    }

    @Transactional
    public VisitSummaryResponse updateDraft(AuthenticatedUser principal, Long summaryId, VisitSummaryUpdateRequest request) {
        VisitSummary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit summary not found with ID: " + summaryId));

        validateAndResolveDoctor(principal, summary.getAuthoringDoctor().getId());

        if (summary.getStatus() == VisitSummaryStatus.APPROVED) {
            throw new ConflictException("Approved visit summaries cannot be modified directly; please submit a clinical revision");
        }

        if (request.getChiefComplaint() != null) {
            summary.setChiefComplaint(request.getChiefComplaint());
        }
        if (request.getDiagnosis() != null) {
            summary.setDiagnosis(request.getDiagnosis());
        }
        if (request.getClinicalNotes() != null) {
            summary.setClinicalNotes(request.getClinicalNotes());
        }
        if (request.getTreatmentPlan() != null) {
            summary.setTreatmentPlan(request.getTreatmentPlan());
        }

        VisitSummary saved = summaryRepository.save(summary);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "VISIT_SUMMARY_UPDATED",
                "VISIT_SUMMARY",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Updated draft visit summary " + saved.getSummaryReference()
        );

        return VisitSummaryResponse.from(saved);
    }

    @Transactional
    public VisitSummaryResponse approveSummary(AuthenticatedUser principal, Long summaryId) {
        VisitSummary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit summary not found with ID: " + summaryId));

        Doctor doctor = validateAndResolveDoctor(principal, summary.getAuthoringDoctor().getId());

        if (summary.getStatus() == VisitSummaryStatus.APPROVED) {
            throw new ConflictException("Visit summary is already approved");
        }

        summary.setStatus(VisitSummaryStatus.APPROVED);
        summary.setApprovedAt(Instant.now());
        summary.setApprovedByDoctor(doctor);
        VisitSummary saved = summaryRepository.save(summary);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "VISIT_SUMMARY_APPROVED",
                "VISIT_SUMMARY",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Approved visit summary " + saved.getSummaryReference()
        );

        // Notify patient
        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.VISIT_SUMMARY_READY,
                "Medical Visit Summary Ready",
                String.format("Dr. %s has approved your visit summary for consultation %s.",
                        doctor.getUser().getFullName(), saved.getConsultation().getConsultationReference()),
                saved.getAppointment().getId(),
                null
        );

        log.info("Visit summary approved: reference={} by doctor={}", saved.getSummaryReference(), doctor.getId());
        return VisitSummaryResponse.from(saved);
    }

    @Transactional
    public VisitSummaryResponse reviseSummary(AuthenticatedUser principal, Long summaryId, VisitSummaryRevisionRequest request) {
        VisitSummary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit summary not found with ID: " + summaryId));

        Doctor doctor = validateAndResolveDoctor(principal, summary.getAuthoringDoctor().getId());

        if (summary.getStatus() != VisitSummaryStatus.APPROVED) {
            throw new ConflictException("Only approved visit summaries can be revised");
        }

        // Archive previous approved snapshot
        VisitSummaryRevision revision = VisitSummaryRevision.builder()
                .visitSummary(summary)
                .revisionNumber(summary.getVersionNumber())
                .chiefComplaint(summary.getChiefComplaint())
                .diagnosis(summary.getDiagnosis())
                .clinicalNotes(summary.getClinicalNotes())
                .treatmentPlan(summary.getTreatmentPlan())
                .archivedAt(Instant.now())
                .archivedByDoctor(doctor)
                .revisionReason(request.getRevisionReason())
                .build();
        revisionRepository.save(revision);

        // Update summary with new revision
        if (request.getChiefComplaint() != null) {
            summary.setChiefComplaint(request.getChiefComplaint());
        }
        if (request.getDiagnosis() != null) {
            summary.setDiagnosis(request.getDiagnosis());
        }
        if (request.getClinicalNotes() != null) {
            summary.setClinicalNotes(request.getClinicalNotes());
        }
        if (request.getTreatmentPlan() != null) {
            summary.setTreatmentPlan(request.getTreatmentPlan());
        }

        summary.setVersionNumber(summary.getVersionNumber() + 1);
        summary.setRevisionReason(request.getRevisionReason());
        summary.setApprovedAt(Instant.now());
        summary.setApprovedByDoctor(doctor);
        VisitSummary saved = summaryRepository.save(summary);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "VISIT_SUMMARY_REVISED",
                "VISIT_SUMMARY",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Revised visit summary " + saved.getSummaryReference() + " to version " + saved.getVersionNumber()
        );

        log.info("Visit summary revised: reference={}, newVersion={}", saved.getSummaryReference(), saved.getVersionNumber());
        return VisitSummaryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public VisitSummaryResponse getSummaryById(AuthenticatedUser principal, Long summaryId) {
        VisitSummary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit summary not found with ID: " + summaryId));

        validateViewAccess(principal, summary);
        return VisitSummaryResponse.from(summary);
    }

    @Transactional(readOnly = true)
    public Page<VisitSummaryResponse> getPatientSummaries(AuthenticatedUser principal, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

        // Patient can ONLY view APPROVED summaries
        return summaryRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(patient.getId(), VisitSummaryStatus.APPROVED, pageable)
                .map(VisitSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<VisitSummaryResponse> getDoctorSummaries(AuthenticatedUser principal, Pageable pageable) {
        Doctor doctor = resolveDoctorFromPrincipal(principal);
        return summaryRepository.findByAuthoringDoctorIdOrderByCreatedAtDesc(doctor.getId(), pageable)
                .map(VisitSummaryResponse::from);
    }

    private void validateViewAccess(AuthenticatedUser principal, VisitSummary summary) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }

        if (principal.getRole() == UserRole.PATIENT) {
            if (!summary.getPatient().getUser().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Access denied: You do not have permission to view this medical record");
            }
            if (summary.getStatus() != VisitSummaryStatus.APPROVED) {
                throw new AccessDeniedException("Access denied: Visit summary is still in draft state and has not been approved");
            }
            return;
        }

        if (principal.getRole() == UserRole.DOCTOR) {
            if (summary.getAuthoringDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }

        throw new AccessDeniedException("Access denied: You are not authorized to view this clinical summary");
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

        throw new AccessDeniedException("Access denied: You are not authorized to perform clinical summary operations for doctor ID " + doctorId);
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
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
            }
            String candidate = "VS-" + datePrefix + "-" + sb;
            if (summaryRepository.findBySummaryReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "VS-" + datePrefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
