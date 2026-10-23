package com.mediqueue.department.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.department.dto.DepartmentResponse;
import com.mediqueue.department.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments (Public)", description = "Public read-only directory of active hospital departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "List active departments", description = "Retrieves a paginated list of all active hospital departments.")
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> getActiveDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int boundedSize = Math.min(Math.max(1, size), 100);
        int validPage = Math.max(0, page);
        Pageable pageable = PageRequest.of(validPage, boundedSize, Sort.by("name").ascending());
        PageResponse<DepartmentResponse> response = departmentService.getPublicDepartments(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieves an active hospital department by its identifier.")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getPublicDepartmentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Department retrieved successfully", response));
    }
}
