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
public class DepartmentWorkloadResponse {

    private Long departmentId;
    private String departmentName;
    private long totalAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private long activeDoctorsCount;
}
