package com.mediqueue.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.common.PageResponse;
import com.mediqueue.department.controller.AdminDepartmentController;
import com.mediqueue.department.controller.DepartmentController;
import com.mediqueue.department.dto.DepartmentCreateRequest;
import com.mediqueue.department.dto.DepartmentResponse;
import com.mediqueue.department.dto.DepartmentStatusRequest;
import com.mediqueue.department.service.DepartmentService;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.GlobalExceptionHandler;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DepartmentController.class, AdminDepartmentController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/v1/departments should return list of active departments")
    void testGetPublicDepartments() throws Exception {
        DepartmentResponse dept = DepartmentResponse.builder()
                .id(1L)
                .name("Cardiology")
                .description("Heart and vascular care")
                .active(true)
                .build();

        PageResponse<DepartmentResponse> pageResponse = PageResponse.<DepartmentResponse>builder()
                .content(List.of(dept))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(departmentService.getPublicDepartments(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Cardiology"));
    }

    @Test
    @DisplayName("GET /api/v1/departments/{id} should return 404 when department not found")
    void testGetDepartmentNotFound() throws Exception {
        when(departmentService.getPublicDepartmentById(999L))
                .thenThrow(new ResourceNotFoundException("Department not found or inactive with ID: 999"));

        mockMvc.perform(get("/api/v1/departments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/departments should create department")
    void testCreateDepartmentSuccess() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Neurology")
                .description("Brain and nerve care")
                .build();

        DepartmentResponse response = DepartmentResponse.builder()
                .id(2L)
                .name("Neurology")
                .description("Brain and nerve care")
                .active(true)
                .build();

        when(departmentService.createDepartment(any(DepartmentCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Neurology"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/departments should return 409 for duplicate department name")
    void testCreateDuplicateDepartment() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Cardiology")
                .build();

        when(departmentService.createDepartment(any(DepartmentCreateRequest.class)))
                .thenThrow(new ConflictException("Department with name 'Cardiology' already exists"));

        mockMvc.perform(post("/api/v1/admin/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }
}
