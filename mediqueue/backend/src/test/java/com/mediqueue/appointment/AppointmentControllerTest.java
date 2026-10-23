package com.mediqueue.appointment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.appointment.controller.AppointmentController;
import com.mediqueue.appointment.dto.AppointmentBookingRequest;
import com.mediqueue.appointment.dto.AppointmentCancelRequest;
import com.mediqueue.appointment.dto.AppointmentRescheduleRequest;
import com.mediqueue.appointment.dto.AppointmentResponse;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.service.AppointmentService;
import com.mediqueue.common.PageResponse;
import com.mediqueue.exception.GlobalExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserRepository userRepository;

    private void setupPatientAuth() {
        User user = User.builder()
                .id(10L)
                .email("patient@mediqueue.com")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        AuthenticatedUser principal = new AuthenticatedUser(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/v1/appointments should return 201 on success")
    void testBookAppointmentSuccess() throws Exception {
        setupPatientAuth();

        LocalDateTime slotTime = LocalDateTime.of(2026, 10, 20, 10, 0);
        AppointmentBookingRequest request = AppointmentBookingRequest.builder()
                .doctorId(1L)
                .startDateTime(slotTime)
                .reason("Health checkup")
                .build();

        AppointmentResponse response = AppointmentResponse.builder()
                .id(100L)
                .appointmentReference("APT-20261020-XYZ999")
                .doctorId(1L)
                .patientId(5L)
                .startDateTime(slotTime)
                .endDateTime(slotTime.plusMinutes(30))
                .status(AppointmentStatus.BOOKED)
                .build();

        when(appointmentService.bookAppointment(any(), any(AppointmentBookingRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointmentReference").value("APT-20261020-XYZ999"))
                .andExpect(jsonPath("$.data.status").value("BOOKED"));
    }

    @Test
    @DisplayName("GET /api/v1/appointments/my should return patient appointment history")
    void testGetMyAppointments() throws Exception {
        setupPatientAuth();

        AppointmentResponse appt = AppointmentResponse.builder()
                .id(100L)
                .appointmentReference("APT-20261020-XYZ999")
                .status(AppointmentStatus.BOOKED)
                .build();

        PageResponse<AppointmentResponse> page = PageResponse.<AppointmentResponse>builder()
                .content(List.of(appt))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .build();

        when(appointmentService.getMyAppointments(eq(10L), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/appointments/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].appointmentReference").value("APT-20261020-XYZ999"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/{id}/cancel should cancel appointment")
    void testCancelAppointment() throws Exception {
        setupPatientAuth();

        AppointmentCancelRequest request = new AppointmentCancelRequest("Cancelled by patient");
        AppointmentResponse response = AppointmentResponse.builder()
                .id(100L)
                .appointmentReference("APT-20261020-XYZ999")
                .status(AppointmentStatus.CANCELLED)
                .cancellationReason("Cancelled by patient")
                .build();

        when(appointmentService.cancelAppointment(any(), eq(100L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/appointments/100/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/{id}/reschedule should reschedule appointment")
    void testRescheduleAppointment() throws Exception {
        setupPatientAuth();

        LocalDateTime newTime = LocalDateTime.of(2026, 10, 20, 14, 0);
        AppointmentRescheduleRequest request = new AppointmentRescheduleRequest(newTime);

        AppointmentResponse response = AppointmentResponse.builder()
                .id(100L)
                .appointmentReference("APT-20261020-XYZ999")
                .startDateTime(newTime)
                .status(AppointmentStatus.BOOKED)
                .build();

        when(appointmentService.rescheduleAppointment(any(), eq(100L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/appointments/100/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("BOOKED"));
    }
}
