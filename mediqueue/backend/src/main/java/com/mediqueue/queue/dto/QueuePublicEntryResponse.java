package com.mediqueue.queue.dto;

import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueuePriority;
import com.mediqueue.queue.enums.QueueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueuePublicEntryResponse {

    private Integer tokenNumber;
    private QueueStatus status;
    private QueuePriority priorityCategory;
    private Instant checkInTimestamp;
    private Instant calledTimestamp;

    public static QueuePublicEntryResponse from(QueueEntry entry) {
        return QueuePublicEntryResponse.builder()
                .tokenNumber(entry.getTokenNumber())
                .status(entry.getStatus())
                .priorityCategory(entry.getPriorityCategory())
                .checkInTimestamp(entry.getCheckInTimestamp())
                .calledTimestamp(entry.getCalledTimestamp())
                .build();
    }
}
