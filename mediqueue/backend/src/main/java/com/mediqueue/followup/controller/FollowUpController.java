package com.mediqueue.followup.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.followup.dto.FollowUpCreateRequest;
import com.mediqueue.followup.dto.FollowUpResponse;
import com.mediqueue.followup.service.FollowUpService;
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
@Tag(name = "Follow-ups", description = "Clinician follow-up scheduling, completion, and patient reminders")
public class FollowUpController {

    private final FollowUpService followUpService;

    @PostMapping("/follow-ups")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Create follow-up recommendation")
    public ResponseEntity<ApiResponse<FollowUpResponse>> createFollowUp(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody FollowUpCreateRequest request
    ) {
        FollowUpResponse response = followUpService.createFollowUp(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Follow-up created successfully"));
    }

    @PatchMapping("/follow-ups/{id}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Mark follow-up completed")
    public ResponseEntity<ApiResponse<FollowUpResponse>> completeFollowUp(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        FollowUpResponse response = followUpService.completeFollowUp(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Follow-up marked completed"));
    }

    @PatchMapping("/follow-ups/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Cancel follow-up")
    public ResponseEntity<ApiResponse<FollowUpResponse>> cancelFollowUp(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        FollowUpResponse response = followUpService.cancelFollowUp(principal, id, reason);
        return ResponseEntity.ok(ApiResponse.success(response, "Follow-up cancelled"));
    }

    @GetMapping("/follow-ups/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get authenticated patient's follow-ups with pagination")
    public ResponseEntity<ApiResponse<PageResponse<FollowUpResponse>>> getMyFollowUps(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<FollowUpResponse> page = followUpService.getPatientFollowUps(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Follow-ups retrieved successfully"));
    }

    @GetMapping("/follow-ups/{id}")
    @Operation(summary = "Get follow-up details by ID")
    public ResponseEntity<ApiResponse<FollowUpResponse>> getFollowUpById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        FollowUpResponse response = followUpService.getFollowUpById(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Follow-up retrieved successfully"));
    }

    @GetMapping("/doctor/follow-ups")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Get doctor follow-ups with pagination")
    public ResponseEntity<ApiResponse<PageResponse<FollowUpResponse>>> getDoctorFollowUps(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<FollowUpResponse> page = followUpService.getDoctorFollowUps(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Doctor follow-ups retrieved"));
    }
}
