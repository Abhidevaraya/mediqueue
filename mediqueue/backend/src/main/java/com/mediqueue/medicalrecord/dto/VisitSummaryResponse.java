package com.mediqueue.medicalrecord.dto;

import com.mediqueue.medicalrecord.entity.VisitSummary;
import com.mediqueue.medicalrecord.enums.VisitSummaryStatus;
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
public class VisitSummaryResponse {

    private Long id;
    private String summaryReference;
    private Long consultationId;
    private String consultationReference;
    private Long appointmentId;
    private String appointmentReference;
    private Long patientId;
    private String patientName;
    private Long authoringDoctorId;
    private String authoringDoctorName;
    private String chiefComplaint;
    private String diagnosis;
    private String clinicalNotes;
    private String treatmentPlan;
    private VisitSummaryStatus status;
    private Instant approvedAt;
    private Long approvedByDoctorId;
    private Integer versionNumber;
    private String revisionReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static VisitSummaryResponse from(VisitSummary summary) {
        return VisitSummaryResponse.builder()
                .id(summary.getId())
                .summaryReference(summary.getSummaryReference())
                .consultationId(summary.getConsultation().getId())
                .consultationReference(summary.getConsultation().getConsultationReference())
                .appointmentId(summary.getAppointment().getId())
                .appointmentReference(summary.getAppointment().getAppointmentReference())
                .patientId(summary.getPatient().getId())
                .patientName(summary.getPatient().getUser().getFullName())
                .authoringDoctorId(summary.getAuthoringDoctor().getId())
                .authoringDoctorName(summary.getAuthoringDoctor().getUser().getFullName())
                .chiefComplaint(summary.getChiefComplaint())
                .diagnosis(summary.getDiagnosis())
                .clinicalNotes(summary.getClinicalNotes())
                .treatmentPlan(summary.getTreatmentPlan())
                .status(summary.getStatus())
                .approvedAt(summary.getApprovedAt())
                .approvedByDoctorId(summary.getApprovedByDoctor() != null ? summary.getApprovedByDoctor().getId() : null)
                .versionNumber(summary.getVersionNumber())
                .revisionReason(summary.getRevisionReason())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }
}
