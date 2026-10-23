package com.mediqueue.doctor.dto;

import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorVerificationRequest {

    @NotNull(message = "Verification status is required")
    private DoctorVerificationStatus verificationStatus;
}
