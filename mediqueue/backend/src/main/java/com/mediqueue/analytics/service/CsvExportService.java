package com.mediqueue.analytics.service;

import com.mediqueue.analytics.dto.AppointmentAnalyticsResponse;
import com.mediqueue.analytics.dto.WaitTimeAnalyticsResponse;
import com.mediqueue.audit.entity.ClinicalAuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class CsvExportService {

    private static final String FORMULA_CHARACTERS = "=+-@\t\r";

    public byte[] exportAppointmentsAnalytics(AppointmentAnalyticsResponse response) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // CSV Header
            writer.println("Date,Total,Booked,Completed,Cancelled");

            if (response.getDailyBreakdown() != null) {
                for (AppointmentAnalyticsResponse.DailyAppointmentMetric m : response.getDailyBreakdown()) {
                    writer.println(String.format("%s,%d,%d,%d,%d",
                            escapeCsv(m.getDate().toString()),
                            m.getTotal(),
                            m.getBooked(),
                            m.getCompleted(),
                            m.getCancelled()
                    ));
                }
            }

            // Summary row
            writer.println(String.format("TOTAL,%d,%d,%d,%d",
                    response.getTotalAppointments(),
                    response.getBookedCount(),
                    response.getCompletedCount(),
                    response.getCancelledCount()
            ));

            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generating appointments CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    public byte[] exportWaitTimesAnalytics(WaitTimeAnalyticsResponse response) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            writer.println("Metric,Value");
            writer.println("From Date," + escapeCsv(response.getFromDate() != null ? response.getFromDate().toString() : ""));
            writer.println("To Date," + escapeCsv(response.getToDate() != null ? response.getToDate().toString() : ""));
            writer.println("Total Samples," + response.getTotalSamples());
            writer.println("Average Wait (mins)," + response.getAverageWaitTimeMinutes());
            writer.println("Median Wait (mins)," + response.getMedianWaitTimeMinutes());
            writer.println("P90 Wait (mins)," + response.getP90WaitTimeMinutes());
            writer.println("Min Wait (mins)," + response.getMinWaitTimeMinutes());
            writer.println("Max Wait (mins)," + response.getMaxWaitTimeMinutes());
            writer.println("Mean Absolute Prediction Error (mins)," + response.getMeanAbsolutePredictionErrorMinutes());

            if (response.getPredictionMethodBreakdown() != null) {
                writer.println("\nPrediction Method,Sample Count");
                response.getPredictionMethodBreakdown().forEach((method, count) ->
                        writer.println(escapeCsv(method) + "," + count));
            }

            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generating wait times CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    public byte[] exportAuditLogs(List<ClinicalAuditEvent> events) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            writer.println("Timestamp,Actor ID,Actor Role,Action,Resource Type,Resource ID,Patient ID,Outcome,Reason");

            for (ClinicalAuditEvent event : events) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        escapeCsv(event.getEventTimestamp() != null ? event.getEventTimestamp().toString() : ""),
                        escapeCsv(event.getActorUserId() != null ? event.getActorUserId().toString() : "SYSTEM"),
                        escapeCsv(event.getActorRole() != null ? event.getActorRole() : ""),
                        escapeCsv(event.getAction()),
                        escapeCsv(event.getResourceType()),
                        escapeCsv(event.getResourceId() != null ? event.getResourceId().toString() : ""),
                        escapeCsv(event.getPatientId() != null ? event.getPatientId().toString() : ""),
                        escapeCsv(event.getOutcome()),
                        escapeCsv(event.getSafeReason() != null ? event.getSafeReason() : "")
                ));
            }

            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error generating audit logs CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    /**
     * Sanitizes CSV field against formula injection and escapes commas/quotes.
     * Characters = , + , - , @ at start of value are neutralized by prepending single quote.
     */
    public String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.stripLeading();
        boolean needsFormulaPrefix = false;

        if (!trimmed.isEmpty()) {
            char firstChar = trimmed.charAt(0);
            if (FORMULA_CHARACTERS.indexOf(firstChar) != -1) {
                needsFormulaPrefix = true;
            }
        }

        String sanitized = needsFormulaPrefix ? "'" + value : value;

        // Standard CSV escaping if field contains comma, quote, or newline
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }

        return sanitized;
    }
}
