package com.mediqueue.medicaldocument.dto;

import com.mediqueue.medicaldocument.entity.MedicalDocument;
import com.mediqueue.medicaldocument.enums.DocumentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDocumentResponse {

    private Long id;
    private String documentReference;
    private Long patientId;
    private String patientName;
    private Long appointmentId;
    private Long consultationId;
    private Long uploadedByUserId;
    private DocumentCategory documentCategory;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private String checksumSha256;
    private Instant createdAt;

    public static MedicalDocumentResponse from(MedicalDocument document) {
        return MedicalDocumentResponse.builder()
                .id(document.getId())
                .documentReference(document.getDocumentReference())
                .patientId(document.getPatient().getId())
                .patientName(document.getPatient().getUser().getFullName())
                .appointmentId(document.getAppointment() != null ? document.getAppointment().getId() : null)
                .consultationId(document.getConsultation() != null ? document.getConsultation().getId() : null)
                .uploadedByUserId(document.getUploadedByUser().getId())
                .documentCategory(document.getDocumentCategory())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .fileSizeBytes(document.getFileSizeBytes())
                .checksumSha256(document.getChecksumSha256())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
