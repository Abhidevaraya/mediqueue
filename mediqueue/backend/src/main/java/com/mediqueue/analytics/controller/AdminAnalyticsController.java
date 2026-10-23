package com.mediqueue.analytics.controller;

import com.mediqueue.analytics.dto.AppointmentAnalyticsResponse;
import com.mediqueue.analytics.dto.DepartmentWorkloadResponse;
import com.mediqueue.analytics.dto.DoctorWorkloadResponse;
import com.mediqueue.analytics.dto.HospitalOverviewResponse;
import com.mediqueue.analytics.dto.WaitTimeAnalyticsResponse;
import com.mediqueue.analytics.service.AnalyticsService;
import com.mediqueue.analytics.service.CsvExportService;
import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.common.ApiResponse;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics (Admin)", description = "Administrative analytics and operational reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;
    private final CsvExportService csvExportService;
    private final ClinicalAuditService auditService;

    @GetMapping("/overview")
    @Operation(summary = "Get hospital operational overview", description = "Retrieves high-level summary of appointments, queues, wait times and consultations for today.")
    public ResponseEntity<ApiResponse<HospitalOverviewResponse>> getOverview() {
        HospitalOverviewResponse response = analyticsService.getOverview();
        return ResponseEntity.ok(ApiResponse.ok("Hospital overview retrieved successfully", response));
    }

    @GetMapping("/appointments")
    @Operation(summary = "Get appointment analytics", description = "Retrieves appointment totals, status breakdown, attendance/cancellation rates, and daily metrics.")
    public ResponseEntity<ApiResponse<AppointmentAnalyticsResponse>> getAppointmentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long departmentId
    ) {
        AppointmentAnalyticsResponse response = analyticsService.getAppointmentAnalytics(from, to, doctorId, departmentId);
        return ResponseEntity.ok(ApiResponse.ok("Appointment analytics retrieved successfully", response));
    }

    @GetMapping("/appointments/export")
    @Operation(summary = "Export appointment analytics CSV", description = "Streams CSV export of appointment metrics with formula-injection sanitization.")
    public ResponseEntity<byte[]> exportAppointmentAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long departmentId,
            @AuthenticationPrincipal AuthenticatedUser adminUser
    ) {
        AppointmentAnalyticsResponse response = analyticsService.getAppointmentAnalytics(from, to, doctorId, departmentId);
        byte[] csvData = csvExportService.exportAppointmentsAnalytics(response);

        auditService.recordEvent(
                adminUser.getId(),
                adminUser.getRole().name(),
                "ANALYTICS_EXPORTED",
                "APPOINTMENT_ANALYTICS",
                null,
                null,
                "SUCCESS",
                "Exported appointment analytics CSV (range: " + from + " to " + to + ")"
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"appointment_analytics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/wait-times")
    @Operation(summary = "Get waiting-time analytics", description = "Retrieves wait-time distribution (average, median, p90) and prediction error evaluation.")
    public ResponseEntity<ApiResponse<WaitTimeAnalyticsResponse>> getWaitTimeAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId
    ) {
        WaitTimeAnalyticsResponse response = analyticsService.getWaitTimeAnalytics(from, to, doctorId);
        return ResponseEntity.ok(ApiResponse.ok("Wait-time analytics retrieved successfully", response));
    }

    @GetMapping("/wait-times/export")
    @Operation(summary = "Export wait-time analytics CSV", description = "Streams CSV export of waiting-time metrics and prediction evaluation.")
    public ResponseEntity<byte[]> exportWaitTimeAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @AuthenticationPrincipal AuthenticatedUser adminUser
    ) {
        WaitTimeAnalyticsResponse response = analyticsService.getWaitTimeAnalytics(from, to, doctorId);
        byte[] csvData = csvExportService.exportWaitTimesAnalytics(response);

        auditService.recordEvent(
                adminUser.getId(),
                adminUser.getRole().name(),
                "ANALYTICS_EXPORTED",
                "WAIT_TIME_ANALYTICS",
                null,
                null,
                "SUCCESS",
                "Exported wait-time analytics CSV (range: " + from + " to " + to + ")"
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"wait_times_analytics.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @GetMapping("/doctors")
    @Operation(summary = "Get doctor workload analytics", description = "Retrieves workload, appointments, and consultation durations per doctor.")
    public ResponseEntity<ApiResponse<List<DoctorWorkloadResponse>>> getDoctorWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long departmentId
    ) {
        List<DoctorWorkloadResponse> response = analyticsService.getDoctorWorkload(from, to, departmentId);
        return ResponseEntity.ok(ApiResponse.ok("Doctor workload analytics retrieved successfully", response));
    }

    @GetMapping("/departments")
    @Operation(summary = "Get department workload analytics", description = "Retrieves workload and appointment outcomes per medical department.")
    public ResponseEntity<ApiResponse<List<DepartmentWorkloadResponse>>> getDepartmentWorkload(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<DepartmentWorkloadResponse> response = analyticsService.getDepartmentWorkload(from, to);
        return ResponseEntity.ok(ApiResponse.ok("Department workload analytics retrieved successfully", response));
    }
}
