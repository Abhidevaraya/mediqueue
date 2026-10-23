package com.mediqueue.auth.controller;

import com.mediqueue.auth.dto.AuthResponse;
import com.mediqueue.auth.dto.LoginRequest;
import com.mediqueue.auth.dto.RegisterRequest;
import com.mediqueue.auth.dto.UserSummaryDto;
import com.mediqueue.auth.service.AuthService;
import com.mediqueue.common.ApiResponse;
import com.mediqueue.doctor.dto.DoctorCreateRequest;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for patient registration, login, logout, and authenticated user details")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new patient account", description = "Public endpoint to register a new patient user and profile.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Patient registered successfully", response));
    }

    @PostMapping("/register-doctor")
    @Operation(summary = "Register new doctor account", description = "Public endpoint to register a new doctor user and profile.")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDoctor(@Valid @RequestBody DoctorCreateRequest request) {
        AuthResponse response = authService.registerDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Doctor registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Authenticates user credentials and returns signed JWT access token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the active JWT access token through cryptographic hash revocation.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user", description = "Returns the profile details of the currently authenticated principal.")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        UserSummaryDto user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.ok("Current user profile retrieved successfully", user));
    }
}
