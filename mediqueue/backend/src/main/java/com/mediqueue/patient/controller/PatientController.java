package com.mediqueue.patient.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.patient.dto.PatientProfileResponse;
import com.mediqueue.patient.dto.PatientProfileUpdateRequest;
import com.mediqueue.patient.service.PatientService;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Profile", description = "Endpoints for managing the authenticated patient profile")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get own patient profile", description = "Retrieves the profile of the currently authenticated patient.")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getMyProfile(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        PatientProfileResponse response = patientService.getMyProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Patient profile retrieved successfully", response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update own patient profile", description = "Updates profile information for the currently authenticated patient.")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PatientProfileUpdateRequest request
    ) {
        PatientProfileResponse response = patientService.updateMyProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Patient profile updated successfully", response));
    }
}
