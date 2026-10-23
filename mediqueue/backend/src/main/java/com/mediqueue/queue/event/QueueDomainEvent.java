package com.mediqueue.queue.event;

import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueEventType;
import com.mediqueue.queue.enums.QueueStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class QueueDomainEvent extends ApplicationEvent {

    private final String eventId;
    private final QueueEventType eventType;
    private final Long doctorId;
    private final LocalDate queueDate;
    private final Long queueEntryId;
    private final Integer tokenNumber;
    private final Long patientUserId;
    private final QueueStatus previousStatus;
    private final QueueStatus newStatus;
    private final boolean isPaused;
    private final String message;
    private final Instant eventTimestamp;

    public QueueDomainEvent(
            Object source,
            QueueEventType eventType,
            Long doctorId,
            LocalDate queueDate,
            QueueEntry entry,
            QueueStatus previousStatus,
            QueueStatus newStatus,
            boolean isPaused,
            String message
    ) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.doctorId = doctorId;
        this.queueDate = queueDate;
        this.queueEntryId = entry != null ? entry.getId() : null;
        this.tokenNumber = entry != null ? entry.getTokenNumber() : null;
        this.patientUserId = entry != null && entry.getPatient() != null && entry.getPatient().getUser() != null
                ? entry.getPatient().getUser().getId()
                : null;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.isPaused = isPaused;
        this.message = message;
        this.eventTimestamp = Instant.now();
    }
}
