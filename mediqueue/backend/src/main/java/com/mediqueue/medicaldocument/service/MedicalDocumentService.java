package com.mediqueue.medicaldocument.service;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.BusinessException;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.medicaldocument.dto.MedicalDocumentResponse;
import com.mediqueue.medicaldocument.entity.MedicalDocument;
import com.mediqueue.medicaldocument.enums.DocumentCategory;
import com.mediqueue.medicaldocument.repository.MedicalDocumentRepository;
import com.mediqueue.medicaldocument.storage.FileStorageService;
import com.mediqueue.patient.entity.Patient;
import com.mediqueue.patient.repository.PatientRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalDocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final MedicalDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRecordRepository consultationRepository;
    private final UserRepository userRepository;
    private final ClinicalAuditService auditService;

    @Transactional
    public MedicalDocumentResponse uploadDocument(
            AuthenticatedUser principal,
            Long patientId,
            Long appointmentId,
            Long consultationId,
            DocumentCategory category,
            MultipartFile file
    ) {
        validateFile(file);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        validateUploadAccess(principal, patient);

        User uploader = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId).orElse(null);
        }

        ConsultationRecord consultation = null;
        if (consultationId != null) {
            consultation = consultationRepository.findById(consultationId).orElse(null);
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String contentType = file.getContentType();
        long size = file.getSize();
        String checksum = calculateChecksum(file);

        String fileExt = extractExtension(originalName);
        String storageKey = UUID.randomUUID() + (fileExt.isEmpty() ? "" : "." + fileExt);

        try {
            fileStorageService.store(file.getInputStream(), storageKey, contentType, size);
        } catch (IOException e) {
            throw new BusinessException("Failed to read uploaded file: " + e.getMessage());
        }

        MedicalDocument document = MedicalDocument.builder()
                .documentReference(generateReference(LocalDate.now()))
                .patient(patient)
                .appointment(appointment)
                .consultation(consultation)
                .uploadedByUser(uploader)
                .documentCategory(category != null ? category : DocumentCategory.OTHER)
                .originalFileName(originalName)
                .storageKey(storageKey)
                .contentType(contentType)
                .fileSizeBytes(size)
                .checksumSha256(checksum)
                .isDeleted(false)
                .build();

        try {
            MedicalDocument saved = documentRepository.save(document);

            auditService.recordEvent(
                    principal.getId(),
                    principal.getRole().name(),
                    "DOCUMENT_UPLOADED",
                    "MEDICAL_DOCUMENT",
                    saved.getId(),
                    saved.getPatient().getId(),
                    "SUCCESS",
                    "Uploaded document " + saved.getDocumentReference() + " (" + saved.getDocumentCategory() + ")"
            );

            log.info("Medical document uploaded: reference={}, key={}", saved.getDocumentReference(), storageKey);
            return MedicalDocumentResponse.from(saved);
        } catch (Exception e) {
            // Clean up stored file on metadata persistence failure
            fileStorageService.delete(storageKey);
            log.error("Failed to persist document metadata; cleaned up file key {}: {}", storageKey, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public DocumentDownloadPayload downloadDocument(AuthenticatedUser principal, Long documentId) {
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical document not found with ID: " + documentId));

        if (Boolean.TRUE.equals(document.getIsDeleted())) {
            throw new ResourceNotFoundException("Medical document has been deleted");
        }

        validateViewAccess(principal, document);

        Resource resource = fileStorageService.loadAsResource(document.getStorageKey());

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "DOCUMENT_DOWNLOADED",
                "MEDICAL_DOCUMENT",
                document.getId(),
                document.getPatient().getId(),
                "SUCCESS",
                "Downloaded document " + document.getDocumentReference()
        );

        return new DocumentDownloadPayload(resource, document.getOriginalFileName(), document.getContentType());
    }

    @Transactional(readOnly = true)
    public MedicalDocumentResponse getDocumentById(AuthenticatedUser principal, Long documentId) {
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical document not found with ID: " + documentId));

        if (Boolean.TRUE.equals(document.getIsDeleted())) {
            throw new ResourceNotFoundException("Medical document has been deleted");
        }

        validateViewAccess(principal, document);
        return MedicalDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public Page<MedicalDocumentResponse> getPatientDocuments(AuthenticatedUser principal, Pageable pageable) {
        Patient patient = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user"));

        return documentRepository.findByPatientIdAndIsDeletedFalseOrderByCreatedAtDesc(patient.getId(), pageable)
                .map(MedicalDocumentResponse::from);
    }

    @Transactional
    public void deleteDocument(AuthenticatedUser principal, Long documentId) {
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical document not found with ID: " + documentId));

        validateDeleteAccess(principal, document);

        document.setIsDeleted(true);
        document.setDeletedAt(Instant.now());
        documentRepository.save(document);

        fileStorageService.delete(document.getStorageKey());

        auditService.recordEvent(
                principal.getId(),
                principal.getRole().name(),
                "DOCUMENT_DELETED",
                "MEDICAL_DOCUMENT",
                document.getId(),
                document.getPatient().getId(),
                "SUCCESS",
                "Deleted document " + document.getDocumentReference()
        );

        log.info("Medical document deleted: reference={}", document.getDocumentReference());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Cannot upload an empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds maximum permitted limit of 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Unsupported file type: " + contentType + ". Permitted types: PDF, JPEG, PNG");
        }
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "document.pdf";
        }
        return Paths.get(filename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return (idx > 0 && idx < filename.length() - 1) ? filename.substring(idx + 1) : "";
    }

    private String calculateChecksum(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new BusinessException("Failed to compute document checksum: " + e.getMessage());
        }
    }

    private void validateUploadAccess(AuthenticatedUser principal, Patient patient) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (patient.getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            return; // Doctor can upload records for patient
        }
        throw new AccessDeniedException("Access denied: You are not authorized to upload documents for this patient");
    }

    private void validateViewAccess(AuthenticatedUser principal, MedicalDocument document) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (principal.getRole() == UserRole.PATIENT) {
            if (document.getPatient().getUser().getId().equals(principal.getId())) {
                return;
            }
        }
        if (principal.getRole() == UserRole.DOCTOR) {
            return; // Authorized doctor viewing patient record
        }
        throw new AccessDeniedException("Access denied: You do not have permission to view or download this medical document");
    }

    private void validateDeleteAccess(AuthenticatedUser principal, MedicalDocument document) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        if (document.getUploadedByUser().getId().equals(principal.getId())) {
            return; // Original uploader can delete
        }
        throw new AccessDeniedException("Access denied: Only the original uploader or an administrator can delete this document");
    }

    private String generateReference(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(CHAR_POOL.charAt(RANDOM.nextInt(CHAR_POOL.length())));
            }
            String candidate = "DOC-" + datePrefix + "-" + sb;
            if (documentRepository.findByDocumentReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "DOC-" + datePrefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public record DocumentDownloadPayload(Resource resource, String filename, String contentType) {}
}
