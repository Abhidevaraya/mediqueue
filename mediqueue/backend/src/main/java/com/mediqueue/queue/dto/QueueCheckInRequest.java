package com.mediqueue.queue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueCheckInRequest {

    @NotNull(message = "Appointment ID is required for check-in")
    private Long appointmentId;
}
