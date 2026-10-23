package com.mediqueue.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HospitalOverviewResponse {

    private long totalAppointmentsToday;
    private long completedAppointmentsToday;
    private long activeQueueSessionsToday;
    private long waitingPatientsToday;
    private double averageWaitTimeMinutesToday;
    private double averageConsultationDurationMinutesToday;
}
