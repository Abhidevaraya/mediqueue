package com.mediqueue.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.exception.GlobalExceptionHandler;
import com.mediqueue.schedule.controller.DoctorScheduleController;
import com.mediqueue.schedule.dto.DoctorScheduleCreateRequest;
import com.mediqueue.schedule.dto.DoctorScheduleResponse;
import com.mediqueue.schedule.dto.SlotResponse;
import com.mediqueue.schedule.service.AvailabilityService;
import com.mediqueue.schedule.service.ScheduleService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DoctorScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DoctorScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private AvailabilityService availabilityService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/v1/doctors/{id}/slots should return slots")
    void testGetDoctorSlots() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 20);
        SlotResponse slot = SlotResponse.builder()
                .startDateTime(LocalDateTime.of(date, LocalTime.of(9, 0)))
                .endDateTime(LocalDateTime.of(date, LocalTime.of(9, 30)))
                .slotDurationMinutes(30)
                .totalCapacity(1)
                .bookedCount(0)
                .remainingCapacity(1)
                .available(true)
                .build();

        when(availabilityService.generateSlots(eq(1L), eq(date))).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/v1/doctors/1/slots")
                        .param("date", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.data[0].available").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/doctor/schedules should create schedule")
    void testCreateDoctorSchedule() throws Exception {
        DoctorScheduleCreateRequest request = DoctorScheduleCreateRequest.builder()
                .doctorId(1L)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(16, 0))
                .slotDurationMinutes(30)
                .slotCapacity(1)
                .build();

        DoctorScheduleResponse response = DoctorScheduleResponse.builder()
                .id(10L)
                .doctorId(1L)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(16, 0))
                .slotDurationMinutes(30)
                .slotCapacity(1)
                .active(true)
                .build();

        when(scheduleService.createSchedule(any(), any(DoctorScheduleCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/doctor/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dayOfWeek").value("WEDNESDAY"));
    }
}
