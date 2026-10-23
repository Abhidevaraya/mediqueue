package com.mediqueue.doctor.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.doctor.dto.DoctorAdminResponse;
import com.mediqueue.doctor.dto.DoctorCreateRequest;
import com.mediqueue.doctor.dto.DoctorStatusRequest;
import com.mediqueue.doctor.dto.DoctorUpdateRequest;
import com.mediqueue.doctor.dto.DoctorVerificationRequest;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors (Admin)", description = "Administrative endpoints for onboarding, verifying, and managing doctors")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final DoctorService doctorService;

    @GetMapping
    @Operation(summary = "List doctors (admin)", description = "Retrieves all doctors with filters for verification status and department.")
    public ResponseEntity<ApiResponse<PageResponse<DoctorAdminResponse>>> getAdminDoctors(
            @RequestParam(required = false) DoctorVerificationStatus verificationStatus,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("id").descending());
        PageResponse<DoctorAdminResponse> response = doctorService.getAdminDoctors(verificationStatus, departmentId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Doctors retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor details (admin)", description = "Retrieves full administrative details of any doctor.")
    public ResponseEntity<ApiResponse<DoctorAdminResponse>> getAdminDoctorById(@PathVariable Long id) {
        DoctorAdminResponse response = doctorService.getAdminDoctorById(id);
        return ResponseEntity.ok(ApiResponse.ok("Doctor details retrieved successfully", response));
    }

    @PostMapping
    @Operation(summary = "Onboard new doctor", description = "Admin endpoint to onboard and provision a doctor account and profile.")
    public ResponseEntity<ApiResponse<DoctorAdminResponse>> createDoctor(
            @Valid @RequestBody DoctorCreateRequest request
    ) {
        DoctorAdminResponse response = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Doctor onboarded successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update doctor details", description = "Updates professional details of a doctor.")
    public ResponseEntity<ApiResponse<DoctorAdminResponse>> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorUpdateRequest request
    ) {
        DoctorAdminResponse response = doctorService.updateDoctor(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Doctor updated successfully", response));
    }

    @PatchMapping("/{id}/verification")
    @Operation(summary = "Update verification status", description = "Updates the verification status of a doctor (e.g., VERIFIED, REJECTED).")
    public ResponseEntity<ApiResponse<DoctorAdminResponse>> updateVerification(
            @PathVariable Long id,
            @Valid @RequestBody DoctorVerificationRequest request
    ) {
        DoctorAdminResponse response = doctorService.updateDoctorVerification(id, request.getVerificationStatus());
        return ResponseEntity.ok(ApiResponse.ok("Doctor verification status updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update doctor account status", description = "Activates or blocks a doctor's login account.")
    public ResponseEntity<ApiResponse<DoctorAdminResponse>> updateAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody DoctorStatusRequest request
    ) {
        DoctorAdminResponse response = doctorService.updateDoctorAccountStatus(id, request.getAccountStatus());
        return ResponseEntity.ok(ApiResponse.ok("Doctor account status updated successfully", response));
    }
}
