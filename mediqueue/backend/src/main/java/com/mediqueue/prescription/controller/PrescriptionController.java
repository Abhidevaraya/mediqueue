package com.mediqueue.prescription.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.prescription.dto.PrescriptionCreateRequest;
import com.mediqueue.prescription.dto.PrescriptionResponse;
import com.mediqueue.prescription.dto.PrescriptionRevisionRequest;
import com.mediqueue.prescription.service.PrescriptionService;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Clinician prescription issuance, revisions, and patient prescription access")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Issue a new digital prescription")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> issuePrescription(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PrescriptionCreateRequest request
    ) {
        PrescriptionResponse response = prescriptionService.issuePrescription(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Prescription issued successfully"));
    }

    @PatchMapping("/prescriptions/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Cancel an issued prescription")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> cancelPrescription(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        PrescriptionResponse response = prescriptionService.cancelPrescription(principal, id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Prescription cancelled successfully"));
    }

    @PostMapping("/prescriptions/{id}/revise")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Revise an issued prescription with linked historical versioning")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> revisePrescription(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRevisionRequest request
    ) {
        PrescriptionResponse response = prescriptionService.revisePrescription(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Prescription revised successfully"));
    }

    @GetMapping("/prescriptions/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get authenticated patient's prescriptions with pagination")
    public ResponseEntity<ApiResponse<PageResponse<PrescriptionResponse>>> getMyPrescriptions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PrescriptionResponse> page = prescriptionService.getPatientPrescriptions(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Patient prescriptions retrieved"));
    }

    @GetMapping("/prescriptions/{id}")
    @Operation(summary = "Get prescription details by ID")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescriptionById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Prescription retrieved successfully"));
    }

    @GetMapping("/doctor/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Get doctor issued prescriptions with pagination")
    public ResponseEntity<ApiResponse<PageResponse<PrescriptionResponse>>> getDoctorPrescriptions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PrescriptionResponse> page = prescriptionService.getDoctorPrescriptions(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Doctor prescriptions retrieved"));
    }
}
