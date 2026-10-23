package com.mediqueue.consultation.dto;

import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.enums.ConsultationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {

    private Long id;
    private String consultationReference;
    private Long appointmentId;
    private String appointmentReference;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private ConsultationStatus status;
    private Instant actualStartTimestamp;
    private Instant actualEndTimestamp;
    private Long durationMinutes;
    private String cancellationReason;
    private Instant createdAt;

    public static ConsultationResponse from(ConsultationRecord record) {
        Long minutes = null;
        if (record.getActualStartTimestamp() != null && record.getActualEndTimestamp() != null) {
            minutes = Duration.between(record.getActualStartTimestamp(), record.getActualEndTimestamp()).toMinutes();
        }

        return ConsultationResponse.builder()
                .id(record.getId())
                .consultationReference(record.getConsultationReference())
                .appointmentId(record.getAppointment().getId())
                .appointmentReference(record.getAppointment().getAppointmentReference())
                .patientId(record.getPatient().getId())
                .patientName(record.getPatient().getUser().getFullName())
                .doctorId(record.getDoctor().getId())
                .doctorName(record.getDoctor().getUser().getFullName())
                .status(record.getStatus())
                .actualStartTimestamp(record.getActualStartTimestamp())
                .actualEndTimestamp(record.getActualEndTimestamp())
                .durationMinutes(minutes)
                .cancellationReason(record.getCancellationReason())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
