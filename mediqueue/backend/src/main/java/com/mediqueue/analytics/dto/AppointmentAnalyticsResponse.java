package com.mediqueue.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAnalyticsResponse {

    private LocalDate fromDate;
    private LocalDate toDate;
    private long totalAppointments;
    private long bookedCount;
    private long checkedInCount;
    private long inProgressCount;
    private long completedCount;
    private long cancelledCount;
    private long noShowCount;
    private double attendanceRatePercent;
    private double cancellationRatePercent;
    private List<DailyAppointmentMetric> dailyBreakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyAppointmentMetric {
        private LocalDate date;
        private long total;
        private long completed;
        private long cancelled;
        private long booked;
    }
}
