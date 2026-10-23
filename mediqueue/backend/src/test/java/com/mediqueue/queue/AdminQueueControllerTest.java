package com.mediqueue.queue;

import com.mediqueue.audit.service.ClinicalAuditService;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.queue.controller.AdminQueueController;
import com.mediqueue.queue.dto.QueueSessionResponse;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import com.mediqueue.queue.service.QueueService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.security.JwtAuthenticationFilter;
import com.mediqueue.security.JwtService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQueueController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorQueueSessionRepository sessionRepository;

    @MockBean
    private QueueEntryRepository queueEntryRepository;

    @MockBean
    private DoctorRepository doctorRepository;

    @MockBean
    private QueueService queueService;

    @MockBean
    private ClinicalAuditService auditService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("Admin can retrieve hospital queue sessions")
    void getQueues_success() throws Exception {
        LocalDate today = LocalDate.now();
        User docUser = User.builder().id(10L).fullName("Dr. Gregory House").build();
        Doctor doctor = Doctor.builder().id(5L).user(docUser).build();
        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .isPaused(false)
                .lastTokenNumber(10)
                .build();

        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(queueEntryRepository.findByDoctorIdAndQueueDateOrderByCheckInTimestampAsc(eq(5L), eq(today)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/admin/queues")
                        .param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].doctorId").value(5))
                .andExpect(jsonPath("$.data[0].doctorName").value("Dr. Gregory House"))
                .andExpect(jsonPath("$.data[0].isPaused").value(false));
    }

    @Test
    @DisplayName("Admin can pause and resume a queue session")
    void pauseAndResumeQueue_success() throws Exception {
        LocalDate today = LocalDate.now();
        Doctor doctor = Doctor.builder().id(5L).build();
        DoctorQueueSession session = DoctorQueueSession.builder()
                .id(1L)
                .doctor(doctor)
                .queueDate(today)
                .isPaused(false)
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(DoctorQueueSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(queueService.pauseQueue(any(), eq(5L), any())).thenReturn(
                QueueSessionResponse.builder().doctorId(5L).queueDate(today).isPaused(true).build()
        );
        when(queueService.resumeQueue(any(), eq(5L))).thenReturn(
                QueueSessionResponse.builder().doctorId(5L).queueDate(today).isPaused(false).build()
        );

        mockMvc.perform(post("/api/v1/admin/queues/1/pause")
                        .param("reason", "Doctor emergency meeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPaused").value(true));

        mockMvc.perform(post("/api/v1/admin/queues/1/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPaused").value(false));
    }
}
