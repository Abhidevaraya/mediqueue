package com.mediqueue.user.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.dto.UpdateAccountStatusRequest;
import com.mediqueue.user.dto.UserResponse;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Users (Admin)", description = "Administrative endpoints for viewing and moderating user accounts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Retrieves all registered users with optional search, role and account status filters.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("id").descending());
        PageResponse<UserResponse> response;
        if (search != null && !search.isBlank()) {
            response = userService.searchUsers(role, status, search.trim(), pageable);
        } else {
            response = userService.getUsers(role, status, pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details", description = "Retrieves safe administrative account details of a specific user.")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok("User retrieved successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user account status", description = "Blocks or activates a user account, with protection against admin self-lockout.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        UserResponse response = userService.updateUserStatus(id, adminUser.getId(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("User account status updated successfully", response));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "Updates a user's role with protection against admin self-demotion.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser adminUser,
            @Valid @RequestBody com.mediqueue.user.dto.UpdateUserRoleRequest request
    ) {
        UserResponse response = userService.updateUserRole(id, adminUser.getId(), request.getRole());
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", response));
    }
}
