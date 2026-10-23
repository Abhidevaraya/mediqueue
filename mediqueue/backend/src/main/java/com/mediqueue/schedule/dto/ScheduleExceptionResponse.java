package com.mediqueue.schedule.dto;

import com.mediqueue.schedule.entity.ScheduleException;
import com.mediqueue.schedule.enums.ScheduleExceptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleExceptionResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private LocalDate exceptionDate;
    private ScheduleExceptionType exceptionType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private Integer slotCapacity;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;

    public static ScheduleExceptionResponse from(ScheduleException exception) {
        return ScheduleExceptionResponse.builder()
                .id(exception.getId())
                .doctorId(exception.getDoctor().getId())
                .doctorName(exception.getDoctor().getUser().getFullName())
                .exceptionDate(exception.getExceptionDate())
                .exceptionType(exception.getExceptionType())
                .startTime(exception.getStartTime())
                .endTime(exception.getEndTime())
                .slotDurationMinutes(exception.getSlotDurationMinutes())
                .slotCapacity(exception.getSlotCapacity())
                .reason(exception.getReason())
                .createdAt(exception.getCreatedAt())
                .updatedAt(exception.getUpdatedAt())
                .build();
    }
}
