package com.mediqueue.appointment.controller;

import com.mediqueue.appointment.dto.AppointmentBookingRequest;
import com.mediqueue.appointment.dto.AppointmentCancelRequest;
import com.mediqueue.appointment.dto.AppointmentRescheduleRequest;
import com.mediqueue.appointment.dto.AppointmentResponse;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.service.AppointmentService;
import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Endpoints for booking, cancelling, rescheduling, and reviewing hospital appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book appointment", description = "Authenticated patient books an appointment for an available doctor slot.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AppointmentBookingRequest request
    ) {
        AppointmentResponse response = appointmentService.bookAppointment(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Appointment booked successfully", response));
    }

    @GetMapping("/appointments/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get patient appointment history", description = "Retrieves paginated list of appointments for the authenticated patient.")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getMyAppointments(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("startDateTime").descending());

        PageResponse<AppointmentResponse> response = appointmentService.getMyAppointments(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Appointments retrieved successfully", response));
    }

    @GetMapping("/appointments/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Get appointment by ID", description = "Retrieves details of an appointment if authorized as owner patient, assigned doctor, or admin.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        AppointmentResponse response = appointmentService.getAppointmentById(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Appointment retrieved successfully", response));
    }

    @PatchMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Cancel appointment", description = "Cancels a booked appointment and releases slot inventory atomically.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AppointmentCancelRequest request
    ) {
        AppointmentResponse response = appointmentService.cancelAppointment(principal, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled successfully", response));
    }

    @PatchMapping("/appointments/{id}/reschedule")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Reschedule appointment", description = "Patient owner or Administrator reschedules an existing appointment to a new available slot.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRescheduleRequest request
    ) {
        AppointmentResponse response = appointmentService.rescheduleAppointment(principal, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Appointment rescheduled successfully", response));
    }

    @GetMapping("/doctor/appointments/today")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Get doctor's daily appointments", description = "Retrieves today's appointments for the authenticated doctor using the hospital timezone.")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getDoctorTodayAppointments(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        List<AppointmentResponse> response = appointmentService.getDoctorTodayAppointments(principal);
        return ResponseEntity.ok(ApiResponse.ok("Today's appointments retrieved successfully", response));
    }

    @GetMapping("/admin/appointments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search all appointments (admin)", description = "Admin endpoint to search appointments across doctors, patients, departments, and date ranges.")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getAdminAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("startDateTime").descending());

        PageResponse<AppointmentResponse> response = appointmentService.getAdminAppointments(
                doctorId, patientId, departmentId, status, startFrom, startTo, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok("Appointments retrieved successfully", response));
    }

    @PatchMapping("/admin/appointments/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Administrative cancellation override", description = "Cancels an appointment administratively.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAdminAppointment(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AppointmentCancelRequest request
    ) {
        AppointmentResponse response = appointmentService.cancelAppointment(principal, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled administratively", response));
    }
}
