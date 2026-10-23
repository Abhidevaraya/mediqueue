package com.mediqueue.appointment.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCancelRequest {

    @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
    private String reason;
}
