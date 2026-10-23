package com.mediqueue.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediqueue.schedule.enums.ScheduleExceptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleExceptionCreateRequest {

    private Long doctorId;

    @NotNull(message = "Exception date is required")
    private LocalDate exceptionDate;

    @NotNull(message = "Exception type is required")
    private ScheduleExceptionType exceptionType;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    private Integer slotDurationMinutes;
    private Integer slotCapacity;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;
}
