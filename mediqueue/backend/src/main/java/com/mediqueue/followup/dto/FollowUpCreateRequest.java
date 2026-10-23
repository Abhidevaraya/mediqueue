package com.mediqueue.followup.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpCreateRequest {

    @NotNull(message = "Consultation ID is required")
    private Long consultationId;

    @NotNull(message = "Recommended follow-up date is required")
    @FutureOrPresent(message = "Recommended date must be today or in the future")
    private LocalDate recommendedDate;

    @Size(max = 500, message = "Instructions cannot exceed 500 characters")
    private String instructions;
}
