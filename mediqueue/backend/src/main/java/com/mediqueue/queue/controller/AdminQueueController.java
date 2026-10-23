package com.mediqueue.queue.controller;

import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.common.ApiResponse;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.queue.dto.AdminQueueSummaryResponse;
import com.mediqueue.queue.dto.QueueSessionResponse;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.service.QueueService;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/queues")
@RequiredArgsConstructor
@Tag(name = "Queues (Admin)", description = "Administrative endpoints for monitoring and controlling hospital queue sessions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQueueController {

    private final DoctorQueueSessionRepository sessionRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final DoctorRepository doctorRepository;
    private final QueueService queueService;
    private final ClinicalAuditService auditService;

    @GetMapping
    @Operation(summary = "Monitor queues", description = "Retrieves live queue sessions and token statistics across doctors and departments.")
    public ResponseEntity<ApiResponse<List<AdminQueueSummaryResponse>>> getQueues(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long doctorId
    ) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<DoctorQueueSession> sessions = sessionRepository.findAll().stream()
                .filter(s -> queryDate.equals(s.getQueueDate()))
                .filter(s -> doctorId == null || s.getDoctor().getId().equals(doctorId))
                .filter(s -> departmentId == null || (s.getDoctor().getDepartment() != null && s.getDoctor().getDepartment().getId().equals(departmentId)))
                .toList();

        List<AdminQueueSummaryResponse> summaries = new ArrayList<>();

        for (DoctorQueueSession session : sessions) {
            Doctor doc = session.getDoctor();
            List<QueueEntry> entries = queueEntryRepository.findByDoctorIdAndQueueDateOrderByCheckInTimestampAsc(
                    doc.getId(), queryDate
            );

            long waiting = entries.stream().filter(e -> e.getStatus() == QueueStatus.WAITING).count();
            long called = entries.stream().filter(e -> e.getStatus() == QueueStatus.CALLED).count();
            long inProg = entries.stream().filter(e -> e.getStatus() == QueueStatus.IN_PROGRESS).count();
            long skipped = entries.stream().filter(e -> e.getStatus() == QueueStatus.SKIPPED).count();
            long completed = entries.stream().filter(e -> e.getStatus() == QueueStatus.COMPLETED).count();
            long cancelled = entries.stream().filter(e -> e.getStatus() == QueueStatus.CANCELLED).count();

            summaries.add(AdminQueueSummaryResponse.builder()
                    .sessionId(session.getId())
                    .doctorId(doc.getId())
                    .doctorName(doc.getUser() != null ? doc.getUser().getFullName() : "Dr. " + doc.getId())
                    .departmentId(doc.getDepartment() != null ? doc.getDepartment().getId() : null)
                    .departmentName(doc.getDepartment() != null ? doc.getDepartment().getName() : "N/A")
                    .sessionDate(session.getQueueDate())
                    .isPaused(session.getIsPaused())
                    .currentCalledToken(session.getCurrentCalledToken())
                    .currentInProgressToken(session.getCurrentInProgressToken())
                    .lastTokenNumber(session.getLastTokenNumber())
                    .waitingCount(waiting)
                    .calledCount(called)
                    .inProgressCount(inProg)
                    .skippedCount(skipped)
                    .completedCount(completed)
                    .cancelledCount(cancelled)
                    .totalPatients(entries.size())
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.ok("Queue sessions retrieved successfully", summaries));
    }

    @PostMapping("/{sessionId}/pause")
    @Operation(summary = "Administrative queue pause", description = "Pauses a doctor's queue session for operational emergencies.")
    public ResponseEntity<ApiResponse<QueueSessionResponse>> pauseQueue(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthenticatedUser adminUser
    ) {
        DoctorQueueSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue session not found with ID: " + sessionId));

        session.setIsPaused(true);
        DoctorQueueSession updated = sessionRepository.save(session);
        if (updated == null) {
            updated = session;
        }
        Long actorId = adminUser != null ? adminUser.getId() : 0L;
        String actorRole = adminUser != null && adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN";

        auditService.recordEvent(
                actorId,
                actorRole,
                "ADMIN_QUEUE_PAUSED",
                "QUEUE_SESSION",
                sessionId,
                null,
                "SUCCESS",
                "Doctor " + session.getDoctor().getId() + " queue paused administratively"
        );

        return ResponseEntity.ok(ApiResponse.ok("Queue session paused", QueueSessionResponse.from(updated)));
    }

    @PostMapping("/{sessionId}/resume")
    @Operation(summary = "Administrative queue resume", description = "Resumes a paused doctor queue session.")
    public ResponseEntity<ApiResponse<QueueSessionResponse>> resumeQueue(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthenticatedUser adminUser
    ) {
        DoctorQueueSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue session not found with ID: " + sessionId));

        session.setIsPaused(false);
        DoctorQueueSession updated = sessionRepository.save(session);
        if (updated == null) {
            updated = session;
        }
        Long actorId = adminUser != null ? adminUser.getId() : 0L;
        String actorRole = adminUser != null && adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN";

        auditService.recordEvent(
                actorId,
                actorRole,
                "ADMIN_QUEUE_RESUMED",
                "QUEUE_SESSION",
                sessionId,
                null,
                "SUCCESS",
                "Doctor " + session.getDoctor().getId() + " queue resumed administratively"
        );

        return ResponseEntity.ok(ApiResponse.ok("Queue session resumed", QueueSessionResponse.from(updated)));
    }
}
