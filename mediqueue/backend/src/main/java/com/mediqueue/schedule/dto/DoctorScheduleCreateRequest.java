package com.mediqueue.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleCreateRequest {

    private Long doctorId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    @NotNull(message = "Slot duration is required")
    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    @Max(value = 180, message = "Slot duration cannot exceed 180 minutes")
    @Builder.Default
    private Integer slotDurationMinutes = 15;

    @NotNull(message = "Slot capacity is required")
    @Min(value = 1, message = "Slot capacity must be at least 1")
    @Max(value = 100, message = "Slot capacity cannot exceed 100")
    @Builder.Default
    private Integer slotCapacity = 1;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime breakStartTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime breakEndTime;

    @Builder.Default
    private Boolean active = true;
}
