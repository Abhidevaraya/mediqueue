package com.mediqueue.followup.dto;

import com.mediqueue.followup.entity.FollowUp;
import com.mediqueue.followup.enums.FollowUpReminderStatus;
import com.mediqueue.followup.enums.FollowUpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpResponse {

    private Long id;
    private String followUpReference;
    private Long consultationId;
    private String consultationReference;
    private Long appointmentId;
    private String appointmentReference;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private LocalDate recommendedDate;
    private String instructions;
    private FollowUpStatus status;
    private FollowUpReminderStatus reminderStatus;
    private Instant reminderSentAt;
    private Instant completedAt;
    private String cancellationReason;
    private Instant createdAt;

    public static FollowUpResponse from(FollowUp followUp) {
        return FollowUpResponse.builder()
                .id(followUp.getId())
                .followUpReference(followUp.getFollowUpReference())
                .consultationId(followUp.getConsultation().getId())
                .consultationReference(followUp.getConsultation().getConsultationReference())
                .appointmentId(followUp.getAppointment().getId())
                .appointmentReference(followUp.getAppointment().getAppointmentReference())
                .patientId(followUp.getPatient().getId())
                .patientName(followUp.getPatient().getUser().getFullName())
                .doctorId(followUp.getDoctor().getId())
                .doctorName(followUp.getDoctor().getUser().getFullName())
                .recommendedDate(followUp.getRecommendedDate())
                .instructions(followUp.getInstructions())
                .status(followUp.getStatus())
                .reminderStatus(followUp.getReminderStatus())
                .reminderSentAt(followUp.getReminderSentAt())
                .completedAt(followUp.getCompletedAt())
                .cancellationReason(followUp.getCancellationReason())
                .createdAt(followUp.getCreatedAt())
                .build();
    }
}
