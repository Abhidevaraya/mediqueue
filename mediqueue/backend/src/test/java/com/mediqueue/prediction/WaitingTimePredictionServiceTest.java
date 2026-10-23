package com.mediqueue.prediction;

import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.prediction.dto.WaitTimePredictionResponse;
import com.mediqueue.prediction.repository.PredictionLogRepository;
import com.mediqueue.prediction.service.WaitingTimePredictionService;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingTimePredictionServiceTest {

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private DoctorQueueSessionRepository doctorQueueSessionRepository;

    @Mock
    private PredictionLogRepository predictionLogRepository;

    @InjectMocks
    private WaitingTimePredictionService predictionService;

    private Doctor doctor;
    private LocalDate today;
    private QueueEntry targetEntry;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 10, 20);
        doctor = Doctor.builder().id(15L).build();

        Appointment appointment = Appointment.builder()
                .id(1L)
                .slotDurationMinutes(20)
                .build();

        targetEntry = QueueEntry.builder()
                .id(100L)
                .doctor(doctor)
                .queueDate(today)
                .tokenNumber(5)
                .status(QueueStatus.WAITING)
                .checkInTimestamp(Instant.now().minus(30, ChronoUnit.MINUTES))
                .appointment(appointment)
                .build();
    }

    @Test
    @DisplayName("Prediction uses fallback when insufficient historical consultations exist")
    void calculateWaitTime_usesFallback() {
        when(queueEntryRepository.countWaitingAhead(eq(15L), eq(today), any())).thenReturn(2);
        when(queueEntryRepository.findRecentCompletedConsultations(eq(15L), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(15L, today, QueueStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(15L, today, QueueStatus.CALLED)).thenReturn(Optional.empty());
        when(doctorQueueSessionRepository.findByDoctorIdAndQueueDate(15L, today)).thenReturn(Optional.empty());

        WaitTimePredictionResponse resp = predictionService.calculateWaitTime(targetEntry);

        assertNotNull(resp);
        assertEquals(2, resp.getEligiblePatientsAhead());
        // 2 patients ahead * 20 min slot duration fallback = 40 min
        assertEquals(40, resp.getEstimatedWaitMinutes());
        assertTrue(resp.getUsesFallback());
        assertEquals("SCHEDULE_SLOT_DURATION_FALLBACK", resp.getPredictionMethod());
        assertFalse(resp.getQueuePaused());
    }

    @Test
    @DisplayName("Prediction uses median of historical consultation durations when data exists")
    void calculateWaitTime_usesMedianHistory() {
        when(queueEntryRepository.countWaitingAhead(eq(15L), eq(today), any())).thenReturn(3);

        Instant base = Instant.now().minus(2, ChronoUnit.HOURS);
        // Durations: 12 min, 18 min, 25 min -> Median is 18 min
        QueueEntry c1 = QueueEntry.builder()
                .consultationStartTimestamp(base)
                .consultationEndTimestamp(base.plus(12, ChronoUnit.MINUTES))
                .build();
        QueueEntry c2 = QueueEntry.builder()
                .consultationStartTimestamp(base.plus(20, ChronoUnit.MINUTES))
                .consultationEndTimestamp(base.plus(38, ChronoUnit.MINUTES))
                .build();
        QueueEntry c3 = QueueEntry.builder()
                .consultationStartTimestamp(base.plus(45, ChronoUnit.MINUTES))
                .consultationEndTimestamp(base.plus(70, ChronoUnit.MINUTES))
                .build();

        when(queueEntryRepository.findRecentCompletedConsultations(eq(15L), any(Pageable.class))).thenReturn(List.of(c1, c2, c3));
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(15L, today, QueueStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(15L, today, QueueStatus.CALLED)).thenReturn(Optional.empty());
        when(doctorQueueSessionRepository.findByDoctorIdAndQueueDate(15L, today)).thenReturn(Optional.empty());

        WaitTimePredictionResponse resp = predictionService.calculateWaitTime(targetEntry);

        assertNotNull(resp);
        assertEquals(3, resp.getEligiblePatientsAhead());
        // 3 patients ahead * 18 min median = 54 min
        assertEquals(54, resp.getEstimatedWaitMinutes());
        assertFalse(resp.getUsesFallback());
        assertEquals("DOCTOR_HISTORY_MEDIAN", resp.getPredictionMethod());
    }

    @Test
    @DisplayName("Prediction returns 0 minutes for CALLED or IN_PROGRESS entries")
    void calculateWaitTime_calledOrInProgressIsZero() {
        targetEntry.setStatus(QueueStatus.CALLED);
        WaitTimePredictionResponse resp = predictionService.calculateWaitTime(targetEntry);

        assertEquals(0, resp.getEstimatedWaitMinutes());
        assertEquals("DIRECT_CALL", resp.getPredictionMethod());
    }

    @Test
    @DisplayName("Prediction reflects queue pause state")
    void calculateWaitTime_reflectsPausedQueue() {
        when(queueEntryRepository.countWaitingAhead(eq(15L), eq(today), any())).thenReturn(1);
        when(queueEntryRepository.findRecentCompletedConsultations(eq(15L), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(15L, today, QueueStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(15L, today, QueueStatus.CALLED)).thenReturn(Optional.empty());

        DoctorQueueSession session = DoctorQueueSession.builder()
                .isPaused(true)
                .pauseReason("Doctor on emergency break")
                .build();
        when(doctorQueueSessionRepository.findByDoctorIdAndQueueDate(15L, today)).thenReturn(Optional.of(session));

        WaitTimePredictionResponse resp = predictionService.calculateWaitTime(targetEntry);

        assertTrue(resp.getQueuePaused());
        assertTrue(resp.getMessage().contains("Doctor on emergency break"));
    }
}
