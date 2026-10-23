package com.mediqueue.prescription.service;

import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.notification.enums.NotificationType;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.prescription.dto.PrescriptionCreateRequest;
import com.mediqueue.prescription.dto.PrescriptionItemRequest;
import com.mediqueue.prescription.dto.PrescriptionResponse;
import com.mediqueue.prescription.dto.PrescriptionRevisionRequest;
import com.mediqueue.prescription.entity.Prescription;
import com.mediqueue.prescription.entity.PrescriptionItem;
import com.mediqueue.prescription.enums.PrescriptionStatus;
import com.mediqueue.prescription.repository.PrescriptionRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRecordRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final ClinicalAuditService auditService;

    @Transactional
    public PrescriptionResponse issuePrescription(AuthenticatedUser principal, PrescriptionCreateRequest request) {
        ConsultationRecord consultation = consultationRepository.findById(request.getConsultationId())
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found with ID: " + request.getConsultationId()));

        Doctor doctor = validateAndResolveDoctor(principal, consultation.getDoctor().getId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("A prescription must include at least one medication item");
        }

        Instant now = Instant.now();
        Prescription prescription = Prescription.builder()
                .prescriptionReference(generateReference(LocalDate.now()))
                .consultation(consultation)
                .appointment(consultation.getAppointment())
                .patient(consultation.getPatient())
                .doctor(doctor)
                .status(PrescriptionStatus.ISSUED)
                .issuedAt(now)
                .notes(request.getNotes())
                .versionNumber(1)
                .build();

        int order = 1;
        for (PrescriptionItemRequest itemReq : request.getItems()) {
            validateItem(itemReq);
            PrescriptionItem item = PrescriptionItem.builder()
                    .medicationName(itemReq.getMedicationName().trim())
                    .dosage(itemReq.getDosage().trim())
                    .frequency(itemReq.getFrequency().trim())
                    .durationDays(itemReq.getDurationDays())
                    .route(itemReq.getRoute())
                    .instructions(itemReq.getInstructions())
                    .itemOrder(order++)
                    .build();
            prescription.addItem(item);
        }

        Prescription saved = prescriptionRepository.save(prescription);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "PRESCRIPTION_ISSUED",
                "PRESCRIPTION",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Issued prescription " + saved.getPrescriptionReference() + " with " + saved.getItems().size() + " items"
        );

        // Notify patient
        notificationService.createNotification(
                saved.getPatient().getUser().getId(),
                NotificationType.PRESCRIPTION_ISSUED,
                "New Prescription Issued",
                String.format("Dr. %s has issued a digital prescription (%s).",
                        doctor.getUser().getFullName(), saved.getPrescriptionReference()),
                saved.getAppointment().getId(),
                null
        );

        log.info("Issued prescription: reference={} for patient={}", saved.getPrescriptionReference(), saved.getPatient().getId());
        return PrescriptionResponse.from(saved);
    }

    @Transactional
    public PrescriptionResponse cancelPrescription(AuthenticatedUser principal, Long prescriptionId, String reason) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));

        validateAndResolveDoctor(principal, prescription.getDoctor().getId());

        if (prescription.getStatus() != PrescriptionStatus.ISSUED) {
            throw new ConflictException("Only ISSUED prescriptions can be cancelled. Current status: " + prescription.getStatus());
        }

        prescription.setStatus(PrescriptionStatus.CANCELLED);
        prescription.setCancelledAt(Instant.now());
        prescription.setCancellationReason(reason != null ? reason.trim() : "Cancelled by clinician");
        Prescription saved = prescriptionRepository.save(prescription);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "PRESCRIPTION_CANCELLED",
                "PRESCRIPTION",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Cancelled prescription " + saved.getPrescriptionReference() + ": " + saved.getCancellationReason()
        );

        log.info("Cancelled prescription: reference={}", saved.getPrescriptionReference());
        return PrescriptionResponse.from(saved);
    }

    @Transactional
    public PrescriptionResponse revisePrescription(AuthenticatedUser principal, Long prescriptionId, PrescriptionRevisionRequest request) {
        Prescription existing = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));

        Doctor doctor = validateAndResolveDoctor(principal, existing.getDoctor().getId());

        if (existing.getStatus() != PrescriptionStatus.ISSUED) {
            throw new ConflictException("Only currently ISSUED prescriptions can be revised. Current status: " + existing.getStatus());
        }

        // Mark existing as REVISED
        existing.setStatus(PrescriptionStatus.REVISED);
        prescriptionRepository.save(existing);

        // Create new linked version
        Instant now = Instant.now();
        Prescription newPrescription = Prescription.builder()
                .prescriptionReference(generateReference(LocalDate.now()))
                .consultation(existing.getConsultation())
                .appointment(existing.getAppointment())
                .patient(existing.getPatient())
                .doctor(doctor)
                .status(PrescriptionStatus.ISSUED)
                .issuedAt(now)
                .notes(request.getNotes())
                .versionNumber(existing.getVersionNumber() + 1)
                .parentPrescriptionId(existing.getId())
                .build();

        int order = 1;
        for (PrescriptionItemRequest itemReq : request.getItems()) {
            validateItem(itemReq);
            PrescriptionItem item = PrescriptionItem.builder()
                    .medicationName(itemReq.getMedicationName().trim())
                    .dosage(itemReq.getDosage().trim())
                    .frequency(itemReq.getFrequency().trim())
                    .durationDays(itemReq.getDurationDays())
                    .route(itemReq.getRoute())
                    .instructions(itemReq.getInstructions())
                    .itemOrder(order++)
                    .build();
            newPrescription.addItem(item);
        }

        Prescription saved = prescriptionRepository.save(newPrescription);

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "PRESCRIPTION_REVISED",
                "PRESCRIPTION",
                saved.getId(),
                saved.getPatient().getId(),
                "SUCCESS",
                "Revised prescription " + existing.getPrescriptionReference() + " to new version " + saved.getPrescriptionReference() + " (v" + saved.getVersionNumber() + ")"
        );

        log.info("Revised prescription: oldReference={}, newReference={}", existing.getPrescriptionReference(), saved.getPrescriptionReference());
        return PrescriptionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(AuthenticatedUser principal, Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + prescriptionId));

        validateViewAccess(principal, prescription);
        return PrescriptionResponse.from(prescription);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getPatientPrescriptions(AuthenticatedUser principal, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

        return prescriptionRepository.findByPatientIdOrderByIssuedAtDesc(patient.getId(), pageable)
                .map(PrescriptionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponse> getDoctorPrescriptions(AuthenticatedUser principal, Pageable pageable) {
        Doctor doctor = resolveDoctorFromPrincipal(principal);
        return prescriptionRepository.findByDoctorIdOrderByIssuedAtDesc(doctor.getId(), pageable)
                .map(PrescriptionResponse::from);
    }

    private void validateItem(PrescriptionItemRequest item) {
        if (item.getDurationDays() == null || item.getDurationDays() <= 0) {
            throw new BusinessException("Medication duration must be at least 1 day for " + item.getMedicationName());
        }
    }

    private void validateViewAccess(AuthenticatedUser principal, Prescription prescription) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }

        if (principal.getRole() == UserRole.PATIENT) {
            if (prescription.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }

        if (principal.getRole() == UserRole.DOCTOR) {
            if (prescription.getDoctor().getUser().getId().equals(principal.getId())) {
                return;
            }
        }

        throw new AccessDeniedException("Access denied: You do not have permission to view this prescription");
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

        throw new AccessDeniedException("Access denied: You are not authorized to issue or manage prescriptions for doctor ID " + doctorId);
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
            String candidate = "RX-" + datePrefix + "-" + sb;
            if (prescriptionRepository.findByPrescriptionReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "RX-" + datePrefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
