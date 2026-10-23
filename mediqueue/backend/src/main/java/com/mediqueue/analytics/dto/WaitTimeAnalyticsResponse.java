package com.mediqueue.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitTimeAnalyticsResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private long totalSamples;
    private double averageWaitTimeMinutes;
    private double medianWaitTimeMinutes;
    private double p90WaitTimeMinutes;
    private int minWaitTimeMinutes;
    private int maxWaitTimeMinutes;
    private double meanAbsolutePredictionErrorMinutes;
    private Map<String, Long> predictionMethodBreakdown;
}
