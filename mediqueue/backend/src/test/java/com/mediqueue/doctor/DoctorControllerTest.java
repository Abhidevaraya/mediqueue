package com.mediqueue.doctor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.common.PageResponse;
import com.mediqueue.doctor.controller.AdminDoctorController;
import com.mediqueue.doctor.controller.DoctorController;
import com.mediqueue.doctor.dto.DoctorAdminResponse;
import com.mediqueue.doctor.dto.DoctorCreateRequest;
import com.mediqueue.doctor.dto.DoctorPublicResponse;
import com.mediqueue.doctor.dto.DoctorVerificationRequest;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.doctor.service.DoctorService;
import com.mediqueue.exception.ConflictException;
import com.mediqueue.exception.GlobalExceptionHandler;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.enums.AccountStatus;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DoctorController.class, AdminDoctorController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/v1/doctors should return public verified doctors")
    void testGetPublicDoctors() throws Exception {
        DoctorPublicResponse doc = DoctorPublicResponse.builder()
                .id(1L)
                .fullName("Dr. Sarah Jenkins")
                .specialty("Interventional Cardiology")
                .departmentId(1L)
                .departmentName("Cardiology")
                .qualification("MD, FACC")
                .yearsOfExperience(12)
                .professionalBio("Experienced cardiologist")
                .consultationFee(BigDecimal.valueOf(150.00))
                .build();

        PageResponse<DoctorPublicResponse> pageResponse = PageResponse.<DoctorPublicResponse>builder()
                .content(List.of(doc))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(doctorService.getPublicDoctors(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Dr. Sarah Jenkins"))
                .andExpect(jsonPath("$.data.content[0].specialty").value("Interventional Cardiology"));
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id} should return 404 for unverified or nonexistent doctor")
    void testGetPublicDoctorNotFound() throws Exception {
        when(doctorService.getPublicDoctorById(999L))
                .thenThrow(new ResourceNotFoundException("Doctor not found or not currently available with ID: 999"));

        mockMvc.perform(get("/api/v1/doctors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/doctors should onboard new doctor")
    void testAdminCreateDoctor() throws Exception {
        DoctorCreateRequest request = DoctorCreateRequest.builder()
                .fullName("Dr. Marcus Vance")
                .email("dr.vance@mediqueue.com")
                .password("DoctorPass123!")
                .phoneNumber("+1987654321")
                .departmentId(1L)
                .specialty("Neurology")
                .qualification("MD, PhD")
                .yearsOfExperience(15)
                .consultationFee(BigDecimal.valueOf(200.00))
                .build();

        DoctorAdminResponse response = DoctorAdminResponse.builder()
                .id(2L)
                .userId(20L)
                .fullName("Dr. Marcus Vance")
                .email("dr.vance@mediqueue.com")
                .departmentId(1L)
                .departmentName("Neurology")
                .specialty("Neurology")
                .qualification("MD, PhD")
                .yearsOfExperience(15)
                .consultationFee(BigDecimal.valueOf(200.00))
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(doctorService.createDoctor(any(DoctorCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("dr.vance@mediqueue.com"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/doctors/{id}/verification should update verification status")
    void testUpdateDoctorVerification() throws Exception {
        DoctorVerificationRequest request = DoctorVerificationRequest.builder()
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build();

        DoctorAdminResponse response = DoctorAdminResponse.builder()
                .id(1L)
                .verificationStatus(DoctorVerificationStatus.VERIFIED)
                .build();

        when(doctorService.updateDoctorVerification(eq(1L), eq(DoctorVerificationStatus.VERIFIED)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/doctors/1/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }
}
