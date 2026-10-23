package com.mediqueue.medicalrecord.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.medicalrecord.dto.VisitSummaryCreateRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryResponse;
import com.mediqueue.medicalrecord.dto.VisitSummaryRevisionRequest;
import com.mediqueue.medicalrecord.dto.VisitSummaryUpdateRequest;
import com.mediqueue.medicalrecord.service.VisitSummaryService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Medical Records & Visit Summaries", description = "Clinician-authored clinical summaries and patient health records")
public class MedicalRecordController {

    private final VisitSummaryService summaryService;

    @PostMapping("/medical-records")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Create draft visit summary for a consultation")
    public ResponseEntity<ApiResponse<VisitSummaryResponse>> createDraft(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody VisitSummaryCreateRequest request
    ) {
        VisitSummaryResponse response = summaryService.createDraft(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Draft visit summary created successfully"));
    }

    @PatchMapping("/medical-records/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Update draft visit summary")
    public ResponseEntity<ApiResponse<VisitSummaryResponse>> updateDraft(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody VisitSummaryUpdateRequest request
    ) {
        VisitSummaryResponse response = summaryService.updateDraft(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Draft visit summary updated successfully"));
    }

    @PostMapping("/medical-records/{id}/approve")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Approve clinical visit summary and release to patient")
    public ResponseEntity<ApiResponse<VisitSummaryResponse>> approveSummary(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        VisitSummaryResponse response = summaryService.approveSummary(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Visit summary approved successfully"));
    }

    @PostMapping("/medical-records/{id}/revise")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Revise an approved visit summary with historical archiving")
    public ResponseEntity<ApiResponse<VisitSummaryResponse>> reviseSummary(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody VisitSummaryRevisionRequest request
    ) {
        VisitSummaryResponse response = summaryService.reviseSummary(principal, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Visit summary revised successfully"));
    }

    @GetMapping("/medical-records/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get patient's approved medical records with pagination")
    public ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getMyMedicalRecords(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<VisitSummaryResponse> page = summaryService.getPatientSummaries(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Medical records retrieved successfully"));
    }

    @GetMapping("/medical-records/{id}")
    @Operation(summary = "Get visit summary by ID")
    public ResponseEntity<ApiResponse<VisitSummaryResponse>> getMedicalRecordById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        VisitSummaryResponse response = summaryService.getSummaryById(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Visit summary retrieved successfully"));
    }

    @GetMapping("/doctor/medical-records")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Get clinician visit summaries with pagination")
    public ResponseEntity<ApiResponse<PageResponse<VisitSummaryResponse>>> getDoctorMedicalRecords(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<VisitSummaryResponse> page = summaryService.getDoctorSummaries(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Doctor visit summaries retrieved"));
    }
}
