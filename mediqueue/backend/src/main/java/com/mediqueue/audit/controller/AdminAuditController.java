package com.mediqueue.audit.controller;

import com.mediqueue.analytics.service.CsvExportService;
import com.mediqueue.audit.entity.ClinicalAuditEvent;
import com.mediqueue.audit.repository.ClinicalAuditEventRepository;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs (Admin)", description = "Administrative endpoints for querying and exporting clinical and operational audit trails")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final ClinicalAuditEventRepository auditEventRepository;
    private final ClinicalAuditService auditService;
    private final CsvExportService csvExportService;

    @GetMapping
    @Operation(summary = "Query audit logs", description = "Retrieves paginated audit events with optional filters for action, actor, resource, and time range.")
    public ResponseEntity<ApiResponse<PageResponse<ClinicalAuditEvent>>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String outcome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("eventTimestamp").descending());

        Page<ClinicalAuditEvent> events = auditEventRepository.findFilteredEvents(
                from, to, action, actorId, resourceType, resourceId, outcome, pageable
        );

        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved successfully", PageResponse.from(events)));
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs CSV", description = "Streams a CSV export of filtered audit events with formula-injection neutralization.")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) String outcome,
            @AuthenticationPrincipal AuthenticatedUser adminUser
    ) {
        List<ClinicalAuditEvent> events = auditEventRepository.findFilteredEventsList(
                from, to, action, actorId, resourceType, resourceId, outcome
        );

        byte[] csvData = csvExportService.exportAuditLogs(events);

        Long currentAdminId = adminUser != null ? adminUser.getId() : 0L;
        String actorRole = adminUser != null && adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN";

        auditService.recordEvent(
                currentAdminId,
                actorRole,
                "AUDIT_LOGS_EXPORTED",
                "AUDIT_LOG",
                null,
                null,
                "SUCCESS",
                "Exported " + events.size() + " audit records to CSV"
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
