package com.mediqueue.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminQueueSummaryResponse {

    private Long sessionId;
    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private String departmentName;
    private LocalDate sessionDate;
    private Boolean isPaused;
    private Integer currentCalledToken;
    private Integer currentInProgressToken;
    private Integer lastTokenNumber;
    private long waitingCount;
    private long calledCount;
    private long inProgressCount;
    private long skippedCount;
    private long completedCount;
    private long cancelledCount;
    private long totalPatients;
}
