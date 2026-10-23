package com.mediqueue.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitTimePredictionResponse {

    private Long queueEntryId;
    private Integer tokenNumber;
    private Integer eligiblePatientsAhead;
    private Integer estimatedWaitMinutes;
    private Integer lowerBoundMinutes;
    private Integer upperBoundMinutes;
    private String predictionMethod;
    private Boolean usesFallback;
    private Boolean queuePaused;
    private Instant lastUpdatedAt;
    private String message;
}
