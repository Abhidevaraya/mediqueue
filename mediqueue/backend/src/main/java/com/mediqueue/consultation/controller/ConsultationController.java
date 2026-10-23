package com.mediqueue.consultation.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.consultation.dto.ConsultationResponse;
import com.mediqueue.consultation.service.ConsultationService;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Clinical Consultations", description = "Operations for clinical consultation lifecycle and history")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping("/doctor/consultations/{appointmentId}/start")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Start clinical consultation for an appointment")
    public ResponseEntity<ApiResponse<ConsultationResponse>> startConsultation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long appointmentId
    ) {
        ConsultationResponse response = consultationService.startConsultation(principal, appointmentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation started successfully"));
    }

    @PostMapping("/doctor/consultations/{consultationId}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Complete clinical consultation")
    public ResponseEntity<ApiResponse<ConsultationResponse>> completeConsultation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long consultationId
    ) {
        ConsultationResponse response = consultationService.completeConsultation(principal, consultationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation completed successfully"));
    }

    @GetMapping("/doctor/consultations/{consultationId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Get consultation details (Doctor)")
    public ResponseEntity<ApiResponse<ConsultationResponse>> getDoctorConsultation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long consultationId
    ) {
        ConsultationResponse response = consultationService.getConsultationById(principal, consultationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation retrieved successfully"));
    }

    @GetMapping("/doctor/consultations")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "List doctor consultations with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ConsultationResponse>>> listDoctorConsultations(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ConsultationResponse> page = consultationService.getDoctorConsultations(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Doctor consultations retrieved"));
    }

    @GetMapping("/consultations/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List patient consultation history")
    public ResponseEntity<ApiResponse<PageResponse<ConsultationResponse>>> listPatientConsultations(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ConsultationResponse> page = consultationService.getPatientConsultations(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Patient consultations retrieved"));
    }
}
