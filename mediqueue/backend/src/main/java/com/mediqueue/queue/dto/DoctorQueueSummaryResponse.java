package com.mediqueue.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorQueueSummaryResponse {

    private Long doctorId;
    private String doctorName;
    private LocalDate queueDate;
    private boolean isPaused;
    private String pauseReason;
    private int totalCheckedIn;
    private int waitingCount;
    private int calledCount;
    private int inProgressCount;
    private int completedCount;
    private int skippedCount;
    private Integer currentServingToken;
    private Integer currentCalledToken;
    private List<QueueEntryResponse> entries;
    private List<QueuePublicEntryResponse> publicEntries;
}
