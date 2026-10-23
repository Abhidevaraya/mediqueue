package com.mediqueue.queue.dto;

import com.mediqueue.queue.entity.QueueEvent;
import com.mediqueue.queue.enums.QueueEventType;
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
public class QueueEventResponse {

    private Long id;
    private Long queueEntryId;
    private Integer tokenNumber;
    private Long doctorId;
    private LocalDate queueDate;
    private QueueStatus previousStatus;
    private QueueStatus newStatus;
    private QueueEventType eventType;
    private Long actorUserId;
    private Instant eventTimestamp;
    private String reason;
    private String metadata;

    public static QueueEventResponse from(QueueEvent event) {
        return QueueEventResponse.builder()
                .id(event.getId())
                .queueEntryId(event.getQueueEntry().getId())
                .tokenNumber(event.getQueueEntry().getTokenNumber())
                .doctorId(event.getDoctorId())
                .queueDate(event.getQueueDate())
                .previousStatus(event.getPreviousStatus())
                .newStatus(event.getNewStatus())
                .eventType(event.getEventType())
                .actorUserId(event.getActorUserId())
                .eventTimestamp(event.getEventTimestamp())
                .reason(event.getReason())
                .metadata(event.getMetadata())
                .build();
    }
}
