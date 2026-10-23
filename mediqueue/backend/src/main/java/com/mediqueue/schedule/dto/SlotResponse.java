package com.mediqueue.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponse {

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int slotDurationMinutes;
    private int totalCapacity;
    private int bookedCount;
    private int remainingCapacity;
    private boolean available;
}
