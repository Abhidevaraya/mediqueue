package com.mediqueue.queue.dto;

import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueuePriority;
import com.mediqueue.queue.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntryResponse {

    private Long id;
    private String entryReference;
    private Long appointmentId;
    private String appointmentReference;
    private Long patientId;
    private String patientName;
    private String patientPhoneNumber;
    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private String departmentName;
    private LocalDate queueDate;
    private Integer tokenNumber;
    private QueueStatus status;
    private QueuePriority priorityCategory;
    private Instant checkInTimestamp;
    private Instant calledTimestamp;
    private Instant consultationStartTimestamp;
    private Instant consultationEndTimestamp;
    private String skipReason;
    private String cancellationReason;
    private Instant createdAt;

    public static QueueEntryResponse from(QueueEntry entry) {
        return QueueEntryResponse.builder()
                .id(entry.getId())
                .entryReference(entry.getEntryReference())
                .appointmentId(entry.getAppointment().getId())
                .appointmentReference(entry.getAppointment().getAppointmentReference())
                .patientId(entry.getPatient().getId())
                .patientName(entry.getPatient().getUser().getFullName())
                .patientPhoneNumber(entry.getPatient().getUser().getPhoneNumber())
                .doctorId(entry.getDoctor().getId())
                .doctorName(entry.getDoctor().getUser().getFullName())
                .departmentId(entry.getDepartment().getId())
                .departmentName(entry.getDepartment().getName())
                .queueDate(entry.getQueueDate())
                .tokenNumber(entry.getTokenNumber())
                .status(entry.getStatus())
                .priorityCategory(entry.getPriorityCategory())
                .checkInTimestamp(entry.getCheckInTimestamp())
                .calledTimestamp(entry.getCalledTimestamp())
                .consultationStartTimestamp(entry.getConsultationStartTimestamp())
                .consultationEndTimestamp(entry.getConsultationEndTimestamp())
                .skipReason(entry.getSkipReason())
                .cancellationReason(entry.getCancellationReason())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
