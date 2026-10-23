package com.mediqueue.patient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.exception.GlobalExceptionHandler;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.patient.controller.PatientController;
import com.mediqueue.patient.dto.PatientProfileResponse;
import com.mediqueue.patient.dto.PatientProfileUpdateRequest;
import com.mediqueue.patient.service.PatientService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.security.JwtService;
import com.mediqueue.security.TokenRevocationService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PatientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserRepository userRepository;

    private void setupAuthentication(Long userId) {
        User user = User.builder()
                .id(userId)
                .email("patient@example.com")
                .fullName("Patient User")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/v1/patients/me should return patient profile")
    void testGetMyProfileSuccess() throws Exception {
        setupAuthentication(10L);

        PatientProfileResponse response = PatientProfileResponse.builder()
                .id(1L)
                .userId(10L)
                .fullName("Patient User")
                .email("patient@example.com")
                .gender("MALE")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .address("123 Hospital St")
                .build();

        when(patientService.getMyProfile(10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.email").value("patient@example.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/patients/me should update patient profile")
    void testUpdateMyProfileSuccess() throws Exception {
        setupAuthentication(10L);

        PatientProfileUpdateRequest updateRequest = PatientProfileUpdateRequest.builder()
                .fullName("Patient User Updated")
                .address("456 Clinic Ave")
                .gender("MALE")
                .build();

        PatientProfileResponse updatedResponse = PatientProfileResponse.builder()
                .id(1L)
                .userId(10L)
                .fullName("Patient User Updated")
                .email("patient@example.com")
                .gender("MALE")
                .address("456 Clinic Ave")
                .build();

        when(patientService.updateMyProfile(eq(10L), any(PatientProfileUpdateRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/v1/patients/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Patient User Updated"))
                .andExpect(jsonPath("$.data.address").value("456 Clinic Ave"));
    }
}
