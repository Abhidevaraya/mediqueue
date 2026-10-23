package com.mediqueue.analytics;

import com.mediqueue.analytics.dto.AppointmentAnalyticsResponse;
import com.mediqueue.analytics.dto.WaitTimeAnalyticsResponse;
import com.mediqueue.analytics.service.CsvExportService;
import com.mediqueue.audit.entity.ClinicalAuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private final CsvExportService csvExportService = new CsvExportService();

    @Test
    @DisplayName("Formula injection characters are neutralized with prepended single quote")
    void escapeCsv_neutralizesFormulas() {
        assertThat(csvExportService.escapeCsv("=SUM(A1:A10)")).isEqualTo("'=SUM(A1:A10)");
        assertThat(csvExportService.escapeCsv("+cmd|' /C calc'!A0")).isEqualTo("'+cmd|' /C calc'!A0");
        assertThat(csvExportService.escapeCsv("@SUM(1+2)")).isEqualTo("'@SUM(1+2)");
        assertThat(csvExportService.escapeCsv("@SUM(1,2)")).isEqualTo("\"'@SUM(1,2)\"");
        assertThat(csvExportService.escapeCsv("  =1+1")).isEqualTo("'  =1+1");
        assertThat(csvExportService.escapeCsv("Normal Text")).isEqualTo("Normal Text");
        assertThat(csvExportService.escapeCsv("Text, with comma")).isEqualTo("\"Text, with comma\"");
    }

    @Test
    @DisplayName("Export appointments analytics produces valid CSV with headers")
    void exportAppointmentsAnalytics_success() {
        AppointmentAnalyticsResponse response = AppointmentAnalyticsResponse.builder()
                .fromDate(LocalDate.of(2026, 10, 1))
                .toDate(LocalDate.of(2026, 10, 10))
                .totalAppointments(10)
                .bookedCount(2)
                .completedCount(7)
                .cancelledCount(1)
                .dailyBreakdown(List.of(
                        AppointmentAnalyticsResponse.DailyAppointmentMetric.builder()
                                .date(LocalDate.of(2026, 10, 5))
                                .total(10)
                                .booked(2)
                                .completed(7)
                                .cancelled(1)
                                .build()
                ))
                .build();

        byte[] bytes = csvExportService.exportAppointmentsAnalytics(response);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("Date,Total,Booked,Completed,Cancelled");
        assertThat(csv).contains("2026-10-05,10,2,7,1");
        assertThat(csv).contains("TOTAL,10,2,7,1");
    }

    @Test
    @DisplayName("Export wait times analytics produces valid CSV with metrics")
    void exportWaitTimesAnalytics_success() {
        WaitTimeAnalyticsResponse response = WaitTimeAnalyticsResponse.builder()
                .fromDate(LocalDate.of(2026, 10, 1))
                .toDate(LocalDate.of(2026, 10, 10))
                .totalSamples(20)
                .averageWaitTimeMinutes(14.5)
                .medianWaitTimeMinutes(12.0)
                .p90WaitTimeMinutes(25.0)
                .minWaitTimeMinutes(3)
                .maxWaitTimeMinutes(30)
                .meanAbsolutePredictionErrorMinutes(2.1)
                .predictionMethodBreakdown(Map.of("DOCTOR_HISTORY_MEDIAN", 15L, "DIRECT_CALL", 5L))
                .build();

        byte[] bytes = csvExportService.exportWaitTimesAnalytics(response);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("Average Wait (mins),14.5");
        assertThat(csv).contains("Median Wait (mins),12.0");
        assertThat(csv).contains("DOCTOR_HISTORY_MEDIAN,15");
    }

    @Test
    @DisplayName("Export audit logs produces valid CSV and neutralizes formula in reasons")
    void exportAuditLogs_neutralizesDangerousFields() {
        ClinicalAuditEvent event = ClinicalAuditEvent.builder()
                .id(1L)
                .actorUserId(10L)
                .actorRole("ADMIN")
                .action("USER_STATUS_UPDATED")
                .resourceType("USER")
                .resourceId(20L)
                .outcome("SUCCESS")
                .safeReason("=DANGEROUS_FORMULA()")
                .eventTimestamp(Instant.parse("2026-10-12T10:00:00Z"))
                .build();

        byte[] bytes = csvExportService.exportAuditLogs(List.of(event));
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("Timestamp,Actor ID,Actor Role,Action,Resource Type,Resource ID,Patient ID,Outcome,Reason");
        assertThat(csv).contains("'=DANGEROUS_FORMULA()");
    }
}
