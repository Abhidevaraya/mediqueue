package com.mediqueue.prescription.dto;

import com.mediqueue.prescription.entity.Prescription;
import com.mediqueue.prescription.enums.PrescriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {

    private Long id;
    private String prescriptionReference;
    private Long consultationId;
    private String consultationReference;
    private Long appointmentId;
    private String appointmentReference;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private PrescriptionStatus status;
    private Instant issuedAt;
    private Instant cancelledAt;
    private String cancellationReason;
    private String notes;
    private Integer versionNumber;
    private Long parentPrescriptionId;
    private List<PrescriptionItemResponse> items;
    private Instant createdAt;

    public static PrescriptionResponse from(Prescription prescription) {
        List<PrescriptionItemResponse> itemResponses = prescription.getItems() != null
                ? prescription.getItems().stream().map(PrescriptionItemResponse::from).toList()
                : List.of();

        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .prescriptionReference(prescription.getPrescriptionReference())
                .consultationId(prescription.getConsultation().getId())
                .consultationReference(prescription.getConsultation().getConsultationReference())
                .appointmentId(prescription.getAppointment().getId())
                .appointmentReference(prescription.getAppointment().getAppointmentReference())
                .patientId(prescription.getPatient().getId())
                .patientName(prescription.getPatient().getUser().getFullName())
                .doctorId(prescription.getDoctor().getId())
                .doctorName(prescription.getDoctor().getUser().getFullName())
                .status(prescription.getStatus())
                .issuedAt(prescription.getIssuedAt())
                .cancelledAt(prescription.getCancelledAt())
                .cancellationReason(prescription.getCancellationReason())
                .notes(prescription.getNotes())
                .versionNumber(prescription.getVersionNumber())
                .parentPrescriptionId(prescription.getParentPrescriptionId())
                .items(itemResponses)
                .createdAt(prescription.getCreatedAt())
                .build();
    }
}
