package com.mediqueue.doctor.dto;

import com.mediqueue.user.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorStatusRequest {

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}
