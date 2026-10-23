package com.mediqueue.queue.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.prediction.dto.WaitTimePredictionResponse;
import com.mediqueue.prediction.service.WaitingTimePredictionService;
import com.mediqueue.queue.dto.DoctorQueueSummaryResponse;
import com.mediqueue.queue.dto.QueueActionRequest;
import com.mediqueue.queue.dto.QueueCheckInRequest;
import com.mediqueue.queue.dto.QueueEntryResponse;
import com.mediqueue.queue.dto.QueueEventResponse;
import com.mediqueue.queue.dto.QueueSessionResponse;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.service.QueueService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.enums.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Queue Management", description = "Real-time hospital queue operations, tokens, check-ins, and wait predictions")
public class QueueController {

    private final QueueService queueService;
    private final WaitingTimePredictionService predictionService;
    private final QueueEntryRepository queueEntryRepository;
    private final DoctorRepository doctorRepository;

    @PostMapping("/queue/check-in")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check in appointment", description = "Checks in an eligible confirmed appointment, atomically allocating a sequential queue token.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> checkIn(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody QueueCheckInRequest request
    ) {
        QueueEntryResponse response = queueService.checkIn(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Checked in successfully", response));
    }

    @PostMapping("/appointments/{id}/check-in")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check in appointment by ID", description = "Convenience check-in endpoint using appointment path ID.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> checkInByAppointmentId(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntryResponse response = queueService.checkIn(principal, new QueueCheckInRequest(id));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Checked in successfully", response));
    }

    @GetMapping("/queue/my-status")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my active queue status", description = "Retrieves active queue entry and position for the authenticated patient today.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> getMyActiveQueueStatus(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        QueueEntryResponse response = queueService.getMyActiveQueueStatus(principal);
        return ResponseEntity.ok(ApiResponse.ok("Active queue status retrieved", response));
    }

    @GetMapping("/queue/my-status/wait-estimate")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my waiting time estimate", description = "Calculates data-driven estimated waiting time for the authenticated patient's active entry.")
    public ResponseEntity<ApiResponse<WaitTimePredictionResponse>> getMyWaitTimeEstimate(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        QueueEntryResponse activeStatus = queueService.getMyActiveQueueStatus(principal);
        QueueEntry entry = queueEntryRepository.findById(activeStatus.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));
        WaitTimePredictionResponse estimate = predictionService.calculateWaitTime(entry);
        return ResponseEntity.ok(ApiResponse.ok("Waiting time estimate calculated", estimate));
    }

    @GetMapping("/doctors/{id}/queue/today")
    @Operation(summary = "Get doctor's queue for today", description = "Retrieves today's queue status. Returns full details for doctor/admin, and sanitized token progress for public waiting screens.")
    public ResponseEntity<ApiResponse<DoctorQueueSummaryResponse>> getDoctorQueueToday(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        DoctorQueueSummaryResponse response = queueService.getDoctorQueueToday(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Doctor queue retrieved successfully", response));
    }

    @PostMapping("/doctor/queue/call-next")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Call next waiting patient", description = "Atomically locks and calls the next eligible patient in the doctor's queue.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> callNext(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long doctorId
    ) {
        Long resolvedDoctorId = resolveTargetDoctorId(principal, doctorId);
        QueueEntryResponse response = queueService.callNext(principal, resolvedDoctorId);
        return ResponseEntity.ok(ApiResponse.ok("Next patient called successfully", response));
    }

    @PostMapping("/queue/{id}/call")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Call specific queue entry", description = "Calls a specific waiting patient token directly.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> callEntry(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntryResponse response = queueService.callEntry(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Patient called successfully", response));
    }

    @PostMapping("/queue/{id}/recall")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Recall called patient", description = "Resends notification and refreshes call timestamp for a patient token.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> recallEntry(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntryResponse response = queueService.recallEntry(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Patient recalled successfully", response));
    }

    @PostMapping("/queue/{id}/skip")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Skip absent patient", description = "Marks a patient token as SKIPPED with an audit reason.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> skipEntry(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @RequestBody(required = false) QueueActionRequest request
    ) {
        String reason = request != null ? request.getReason() : null;
        QueueEntryResponse response = queueService.skipEntry(principal, id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Patient skipped successfully", response));
    }

    @PostMapping("/queue/{id}/requeue")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Requeue skipped patient", description = "Places a skipped patient back into the active WAITING line.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> requeueEntry(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntryResponse response = queueService.requeueEntry(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Patient requeued successfully", response));
    }

    @PostMapping("/queue/{id}/start")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Start consultation", description = "Transitions called patient token into IN_PROGRESS status.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> startConsultation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntryResponse response = queueService.startConsultation(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Consultation started successfully", response));
    }

    @PostMapping("/queue/{id}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Complete consultation", description = "Completes the consultation, recording actual duration for waiting-time prediction.")
    public ResponseEntity<ApiResponse<QueueEntryResponse>> completeConsultation(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntryResponse response = queueService.completeConsultation(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Consultation completed successfully", response));
    }

    @PostMapping("/doctor/queue/pause")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Pause doctor queue", description = "Temporarily pauses the doctor's queue, preventing new calls.")
    public ResponseEntity<ApiResponse<QueueSessionResponse>> pauseQueue(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long doctorId,
            @RequestBody(required = false) QueueActionRequest request
    ) {
        Long resolvedDoctorId = resolveTargetDoctorId(principal, doctorId);
        String reason = request != null ? request.getReason() : null;
        QueueSessionResponse response = queueService.pauseQueue(principal, resolvedDoctorId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Doctor queue paused successfully", response));
    }

    @PostMapping("/doctor/queue/resume")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Resume doctor queue", description = "Resumes a paused doctor queue.")
    public ResponseEntity<ApiResponse<QueueSessionResponse>> resumeQueue(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long doctorId
    ) {
        Long resolvedDoctorId = resolveTargetDoctorId(principal, doctorId);
        QueueSessionResponse response = queueService.resumeQueue(principal, resolvedDoctorId);
        return ResponseEntity.ok(ApiResponse.ok("Doctor queue resumed successfully", response));
    }

    @GetMapping("/queue/{id}/events")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get queue entry events", description = "Retrieves the complete audit lifecycle events of a queue entry.")
    public ResponseEntity<ApiResponse<List<QueueEventResponse>>> getQueueEvents(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        List<QueueEventResponse> response = queueService.getQueueEvents(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Queue events retrieved successfully", response));
    }

    @GetMapping("/queue/{id}/wait-estimate")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wait estimate for entry", description = "Calculates waiting time prediction for any specific queue entry.")
    public ResponseEntity<ApiResponse<WaitTimePredictionResponse>> getWaitTimeEstimateForEntry(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        QueueEntry entry = queueEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with ID: " + id));

        // Enforce ownership
        if (principal.getRole() == UserRole.PATIENT && !entry.getPatient().getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Access denied: You cannot view wait predictions for another patient");
        }

        WaitTimePredictionResponse estimate = predictionService.calculateWaitTime(entry);
        return ResponseEntity.ok(ApiResponse.ok("Waiting time estimate calculated", estimate));
    }

    private Long resolveTargetDoctorId(AuthenticatedUser principal, Long paramDoctorId) {
        if (principal.getRole() == UserRole.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Doctor profile not found for authenticated user"));
            return doctor.getId();
        }
        if (principal.getRole() == UserRole.ADMIN) {
            if (paramDoctorId == null) {
                throw new IllegalArgumentException("Administrator must supply doctorId parameter");
            }
            return paramDoctorId;
        }
        throw new AccessDeniedException("Access denied: only doctors or administrators can operate the queue");
    }
}
