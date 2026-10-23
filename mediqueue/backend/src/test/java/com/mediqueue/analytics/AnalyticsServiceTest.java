package com.mediqueue.analytics;

import com.mediqueue.analytics.dto.AppointmentAnalyticsResponse;
import com.mediqueue.analytics.dto.HospitalOverviewResponse;
import com.mediqueue.analytics.dto.WaitTimeAnalyticsResponse;
import com.mediqueue.analytics.service.AnalyticsService;
import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.prediction.entity.PredictionLog;
import com.mediqueue.prediction.repository.PredictionLogRepository;
import com.mediqueue.queue.entity.DoctorQueueSession;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ConsultationRecordRepository consultationRepository;
    @Mock
    private PredictionLogRepository predictionLogRepository;
    @Mock
    private QueueEntryRepository queueEntryRepository;
    @Mock
    private DoctorQueueSessionRepository sessionRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Overview calculates today's metrics without errors")
    void getOverview_calculatesMetrics() {
        LocalDate today = LocalDate.now();

        when(appointmentRepository.countByStartDateTimeBetween(any(), any())).thenReturn(10L);
        when(appointmentRepository.countByStartDateTimeBetweenAndStatus(any(), any(), eq(AppointmentStatus.COMPLETED))).thenReturn(6L);

        DoctorQueueSession session = DoctorQueueSession.builder().queueDate(today).build();
        when(sessionRepository.findAll()).thenReturn(List.of(session));

        when(queueEntryRepository.countByQueueDateAndStatus(eq(today), eq(QueueStatus.WAITING))).thenReturn(4L);

        PredictionLog log1 = PredictionLog.builder().queueDate(today).actualWaitMinutes(12).build();
        PredictionLog log2 = PredictionLog.builder().queueDate(today).actualWaitMinutes(18).build();
        when(predictionLogRepository.findByQueueDate(today)).thenReturn(List.of(log1, log2));

        HospitalOverviewResponse response = analyticsService.getOverview();

        assertThat(response).isNotNull();
        assertThat(response.getTotalAppointmentsToday()).isEqualTo(10L);
        assertThat(response.getCompletedAppointmentsToday()).isEqualTo(6L);
        assertThat(response.getActiveQueueSessionsToday()).isEqualTo(1L);
        assertThat(response.getWaitingPatientsToday()).isEqualTo(4L);
        assertThat(response.getAverageWaitTimeMinutesToday()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Appointment analytics correctly computes attendance and cancellation rates")
    void getAppointmentAnalytics_calculatesRates() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 2);

        Appointment apt1 = Appointment.builder()
                .startDateTime(LocalDateTime.of(2026, 10, 1, 10, 0))
                .status(AppointmentStatus.COMPLETED)
                .build();
        Appointment apt2 = Appointment.builder()
                .startDateTime(LocalDateTime.of(2026, 10, 1, 11, 0))
                .status(AppointmentStatus.CANCELLED)
                .build();
        Appointment apt3 = Appointment.builder()
                .startDateTime(LocalDateTime.of(2026, 10, 2, 9, 0))
                .status(AppointmentStatus.BOOKED)
                .build();

        when(appointmentRepository.findAppointmentsForAnalytics(any(), any(), any(), any()))
                .thenReturn(List.of(apt1, apt2, apt3));

        AppointmentAnalyticsResponse response = analyticsService.getAppointmentAnalytics(from, to, null, null);

        assertThat(response.getTotalAppointments()).isEqualTo(3);
        assertThat(response.getCompletedCount()).isEqualTo(1);
        assertThat(response.getCancelledCount()).isEqualTo(1);
        assertThat(response.getBookedCount()).isEqualTo(1);
        // attendanceRate = completed / (total - cancelled) = 1 / (3 - 1) = 50.0%
        assertThat(response.getAttendanceRatePercent()).isEqualTo(50.0);
        // cancellationRate = cancelled / total = 1 / 3 = 33.3%
        assertThat(response.getCancellationRatePercent()).isEqualTo(33.3);
        assertThat(response.getDailyBreakdown()).hasSize(2);
    }

    @Test
    @DisplayName("Wait time analytics calculates average, median, p90 and MAE error")
    void getWaitTimeAnalytics_calculatesDistributionAndError() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 5);

        PredictionLog p1 = PredictionLog.builder()
                .queueDate(from)
                .predictedWaitMinutes(10)
                .actualWaitMinutes(12)
                .predictionMethod("DOCTOR_HISTORY_MEDIAN")
                .build();
        PredictionLog p2 = PredictionLog.builder()
                .queueDate(from)
                .predictedWaitMinutes(20)
                .actualWaitMinutes(20)
                .predictionMethod("DOCTOR_HISTORY_MEDIAN")
                .build();
        PredictionLog p3 = PredictionLog.builder()
                .queueDate(to)
                .predictedWaitMinutes(15)
                .actualWaitMinutes(30)
                .predictionMethod("SCHEDULE_SLOT_DURATION_FALLBACK")
                .build();

        when(predictionLogRepository.findByQueueDateBetween(from, to)).thenReturn(List.of(p1, p2, p3));

        WaitTimeAnalyticsResponse response = analyticsService.getWaitTimeAnalytics(from, to, null);

        assertThat(response.getTotalSamples()).isEqualTo(3);
        // Wait minutes: [12, 20, 30] -> avg = 62 / 3 = 20.7
        assertThat(response.getAverageWaitTimeMinutes()).isEqualTo(20.7);
        // Median: 20
        assertThat(response.getMedianWaitTimeMinutes()).isEqualTo(20.0);
        // Prediction errors: |10-12|=2, |20-20|=0, |15-30|=15 -> mean = (2+0+15)/3 = 5.7
        assertThat(response.getMeanAbsolutePredictionErrorMinutes()).isEqualTo(5.7);
        assertThat(response.getPredictionMethodBreakdown()).containsEntry("DOCTOR_HISTORY_MEDIAN", 2L);
    }

    @Test
    @DisplayName("Empty datasets return zeroed metrics without division by zero")
    void getAppointmentAnalytics_emptyDataset_zeroedRates() {
        when(appointmentRepository.findAppointmentsForAnalytics(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        AppointmentAnalyticsResponse response = analyticsService.getAppointmentAnalytics(null, null, null, null);

        assertThat(response.getTotalAppointments()).isEqualTo(0);
        assertThat(response.getAttendanceRatePercent()).isEqualTo(0.0);
        assertThat(response.getCancellationRatePercent()).isEqualTo(0.0);
    }
}
