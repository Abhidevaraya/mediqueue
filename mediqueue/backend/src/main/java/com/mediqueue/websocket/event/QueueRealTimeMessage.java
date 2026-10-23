package com.mediqueue.websocket.event;

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
public class QueueRealTimeMessage {

    private String eventId;
    private QueueEventType eventType;
    private Long doctorId;
    private LocalDate queueDate;
    private Long queueEntryId;
    private Integer tokenNumber;
    private QueueStatus currentStatus;
    private Integer publicQueuePosition;
    private Integer estimatedWaitMinutes;
    private boolean isPaused;
    private Instant timestamp;
    private String message;
}
