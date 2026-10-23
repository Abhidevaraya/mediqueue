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
public class DoctorWorkloadResponse {

    private Long doctorId;
    private String doctorName;
    private String specialty;
    private String departmentName;
    private long totalAppointments;
    private long completedConsultations;
    private double averageConsultationMinutes;
    private double medianConsultationMinutes;
}
