package com.mediqueue.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.prediction.service.WaitingTimePredictionService;
import com.mediqueue.queue.controller.QueueController;
import com.mediqueue.queue.dto.DoctorQueueSummaryResponse;
import com.mediqueue.queue.dto.QueueCheckInRequest;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.service.QueueService;
import com.mediqueue.security.JwtAuthenticationFilter;
import com.mediqueue.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QueueService queueService;

    @MockBean
    private WaitingTimePredictionService predictionService;

    @MockBean
    private QueueEntryRepository queueEntryRepository;

    @MockBean
    private DoctorRepository doctorRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/v1/doctors/{id}/queue/today returns 200 OK")
    void getDoctorQueueToday_success() throws Exception {
        DoctorQueueSummaryResponse response = DoctorQueueSummaryResponse.builder()
                .doctorId(1L)
                .doctorName("Dr. Gregory House")
                .queueDate(LocalDate.now())
                .isPaused(false)
                .totalCheckedIn(5)
                .waitingCount(3)
                .calledCount(1)
                .inProgressCount(1)
                .completedCount(0)
                .publicEntries(Collections.emptyList())
                .build();

        when(queueService.getDoctorQueueToday(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/doctors/1/queue/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.doctorName").value("Dr. Gregory House"))
                .andExpect(jsonPath("$.data.waitingCount").value(3));
    }

    @Test
    @DisplayName("POST /api/v1/queue/check-in with null appointmentId returns 400 Bad Request")
    void checkIn_invalidRequest_returns400() throws Exception {
        QueueCheckInRequest badRequest = new QueueCheckInRequest(null);

        mockMvc.perform(post("/api/v1/queue/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }
}
