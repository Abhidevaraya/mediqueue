package com.mediqueue.analytics.service;

import com.mediqueue.analytics.dto.AppointmentAnalyticsResponse;
import com.mediqueue.analytics.dto.DepartmentWorkloadResponse;
import com.mediqueue.analytics.dto.DoctorWorkloadResponse;
import com.mediqueue.analytics.dto.HospitalOverviewResponse;
import com.mediqueue.analytics.dto.WaitTimeAnalyticsResponse;
import com.mediqueue.appointment.entity.Appointment;
import com.mediqueue.appointment.enums.AppointmentStatus;
import com.mediqueue.appointment.repository.AppointmentRepository;
import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.enums.ConsultationStatus;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import com.mediqueue.department.entity.Department;
import com.mediqueue.department.repository.DepartmentRepository;
import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.repository.DoctorRepository;
import com.mediqueue.prediction.entity.PredictionLog;
import com.mediqueue.prediction.repository.PredictionLogRepository;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AppointmentRepository appointmentRepository;
    private final ConsultationRecordRepository consultationRepository;
    private final PredictionLogRepository predictionLogRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final DoctorQueueSessionRepository sessionRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public HospitalOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        long totalAppointmentsToday = appointmentRepository.countByStartDateTimeBetween(startOfDay, endOfDay);
        long completedAppointmentsToday = appointmentRepository.countByStartDateTimeBetweenAndStatus(
                startOfDay, endOfDay, AppointmentStatus.COMPLETED
        );

        long activeSessions = sessionRepository.findAll().stream()
                .filter(s -> today.equals(s.getQueueDate()))
                .count();

        long waitingPatients = queueEntryRepository.countByQueueDateAndStatus(today, QueueStatus.WAITING);

        // Compute average wait time today
        List<PredictionLog> todayLogs = predictionLogRepository.findByQueueDate(today);
        double avgWaitTime = 0.0;
        List<Integer> waitTimes = todayLogs.stream()
                .map(PredictionLog::getActualWaitMinutes)
                .filter(Objects::nonNull)
                .filter(m -> m >= 0)
                .toList();
        if (!waitTimes.isEmpty()) {
            avgWaitTime = waitTimes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        }

        // Compute average consultation duration today
        Instant todayInstantStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant todayInstantEnd = today.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
        List<ConsultationRecord> todayConsultations = consultationRepository.findByActualStartTimestampBetweenAndStatus(
                todayInstantStart, todayInstantEnd, ConsultationStatus.COMPLETED
        );

        double avgConsultation = 0.0;
        List<Long> durations = todayConsultations.stream()
                .filter(c -> c.getActualStartTimestamp() != null && c.getActualEndTimestamp() != null)
                .map(c -> Duration.between(c.getActualStartTimestamp(), c.getActualEndTimestamp()).toMinutes())
                .filter(d -> d > 0 && d <= 180)
                .toList();
        if (!durations.isEmpty()) {
            avgConsultation = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        }

        return HospitalOverviewResponse.builder()
                .totalAppointmentsToday(totalAppointmentsToday)
                .completedAppointmentsToday(completedAppointmentsToday)
                .activeQueueSessionsToday(activeSessions)
                .waitingPatientsToday(waitingPatients)
                .averageWaitTimeMinutesToday(round(avgWaitTime, 1))
                .averageConsultationDurationMinutesToday(round(avgConsultation, 1))
                .build();
    }

    @Transactional(readOnly = true)
    public AppointmentAnalyticsResponse getAppointmentAnalytics(
            LocalDate from,
            LocalDate to,
            Long doctorId,
            Long departmentId
    ) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();

        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);

        List<Appointment> appointments = appointmentRepository.findAppointmentsForAnalytics(
                rangeStart, rangeEnd, doctorId, departmentId
        );

        long total = appointments.size();
        long booked = 0;
        long checkedIn = 0;
        long inProgress = 0;
        long completed = 0;
        long cancelled = 0;
        long noShow = 0;

        Map<LocalDate, long[]> dailyCounts = new TreeMap<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            dailyCounts.put(cur, new long[4]); // [total, completed, cancelled, booked]
            cur = cur.plusDays(1);
        }

        for (Appointment apt : appointments) {
            LocalDate day = apt.getStartDateTime().toLocalDate();
            long[] dayMetrics = dailyCounts.computeIfAbsent(day, k -> new long[4]);
            dayMetrics[0]++; // total

            if (apt.getStatus() == AppointmentStatus.BOOKED) {
                booked++;
                dayMetrics[3]++;
            } else if (apt.getStatus() == AppointmentStatus.CHECKED_IN) {
                checkedIn++;
            } else if (apt.getStatus() == AppointmentStatus.IN_PROGRESS) {
                inProgress++;
            } else if (apt.getStatus() == AppointmentStatus.COMPLETED) {
                completed++;
                dayMetrics[1]++;
            } else if (apt.getStatus() == AppointmentStatus.CANCELLED) {
                cancelled++;
                dayMetrics[2]++;
            } else if (apt.getStatus() == AppointmentStatus.NO_SHOW) {
                noShow++;
            }
        }

        double attendanceRate = 0.0;
        long attendanceDenominator = total - cancelled;
        if (attendanceDenominator > 0) {
            attendanceRate = ((double) completed / attendanceDenominator) * 100.0;
        }

        double cancellationRate = 0.0;
        if (total > 0) {
            cancellationRate = ((double) cancelled / total) * 100.0;
        }

        List<AppointmentAnalyticsResponse.DailyAppointmentMetric> breakdown = dailyCounts.entrySet().stream()
                .map(e -> AppointmentAnalyticsResponse.DailyAppointmentMetric.builder()
                        .date(e.getKey())
                        .total(e.getValue()[0])
                        .completed(e.getValue()[1])
                        .cancelled(e.getValue()[2])
                        .booked(e.getValue()[3])
                        .build())
                .collect(Collectors.toList());

        return AppointmentAnalyticsResponse.builder()
                .fromDate(startDate)
                .toDate(endDate)
                .totalAppointments(total)
                .bookedCount(booked)
                .checkedInCount(checkedIn)
                .inProgressCount(inProgress)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .noShowCount(noShow)
                .attendanceRatePercent(round(attendanceRate, 1))
                .cancellationRatePercent(round(cancellationRate, 1))
                .dailyBreakdown(breakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public WaitTimeAnalyticsResponse getWaitTimeAnalytics(LocalDate from, LocalDate to, Long doctorId) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();

        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        List<PredictionLog> logs = (doctorId != null)
                ? predictionLogRepository.findByQueueDateBetweenAndDoctorId(startDate, endDate, doctorId)
                : predictionLogRepository.findByQueueDateBetween(startDate, endDate);

        List<Integer> waitMinutes = new ArrayList<>();
        List<Double> predictionErrors = new ArrayList<>();
        Map<String, Long> methodBreakdown = new HashMap<>();

        for (PredictionLog log : logs) {
            String method = log.getPredictionMethod() != null ? log.getPredictionMethod() : "UNKNOWN";
            methodBreakdown.put(method, methodBreakdown.getOrDefault(method, 0L) + 1);

            if (log.getActualWaitMinutes() != null && log.getActualWaitMinutes() >= 0) {
                waitMinutes.add(log.getActualWaitMinutes());
                if (log.getPredictedWaitMinutes() != null) {
                    double error = Math.abs(log.getPredictedWaitMinutes() - log.getActualWaitMinutes());
                    predictionErrors.add(error);
                }
            }
        }

        double avgWait = 0.0;
        double medianWait = 0.0;
        double p90Wait = 0.0;
        int minWait = 0;
        int maxWait = 0;
        double mae = 0.0;

        if (!waitMinutes.isEmpty()) {
            Collections.sort(waitMinutes);
            avgWait = waitMinutes.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            minWait = waitMinutes.get(0);
            maxWait = waitMinutes.get(waitMinutes.size() - 1);
            medianWait = calculatePercentile(waitMinutes, 50.0);
            p90Wait = calculatePercentile(waitMinutes, 90.0);
        }

        if (!predictionErrors.isEmpty()) {
            mae = predictionErrors.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        return WaitTimeAnalyticsResponse.builder()
                .fromDate(startDate)
                .toDate(endDate)
                .totalSamples(waitMinutes.size())
                .averageWaitTimeMinutes(round(avgWait, 1))
                .medianWaitTimeMinutes(round(medianWait, 1))
                .p90WaitTimeMinutes(round(p90Wait, 1))
                .minWaitTimeMinutes(minWait)
                .maxWaitTimeMinutes(maxWait)
                .meanAbsolutePredictionErrorMinutes(round(mae, 1))
                .predictionMethodBreakdown(methodBreakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DoctorWorkloadResponse> getDoctorWorkload(LocalDate from, LocalDate to, Long departmentId) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);

        List<Doctor> doctors = (departmentId != null)
                ? doctorRepository.findByDepartmentId(departmentId)
                : doctorRepository.findAll();

        List<DoctorWorkloadResponse> results = new ArrayList<>();

        for (Doctor doc : doctors) {
            List<Appointment> docAppointments = appointmentRepository.findAppointmentsForAnalytics(
                    rangeStart, rangeEnd, doc.getId(), null
            );

            long totalApts = docAppointments.size();
            long completedApts = docAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                    .count();

            Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endInstant = endDate.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
            List<ConsultationRecord> consultations = consultationRepository.findByDoctorIdAndActualStartTimestampBetweenAndStatus(
                    doc.getId(), startInstant, endInstant, ConsultationStatus.COMPLETED
            );

            List<Long> durations = consultations.stream()
                    .filter(c -> c.getActualStartTimestamp() != null && c.getActualEndTimestamp() != null)
                    .map(c -> Duration.between(c.getActualStartTimestamp(), c.getActualEndTimestamp()).toMinutes())
                    .filter(d -> d > 0 && d <= 180)
                    .sorted()
                    .toList();

            double avgDuration = 0.0;
            double medianDuration = 0.0;

            if (!durations.isEmpty()) {
                avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
                medianDuration = durations.get(durations.size() / 2);
            }

            results.add(DoctorWorkloadResponse.builder()
                    .doctorId(doc.getId())
                    .doctorName(doc.getUser() != null ? doc.getUser().getFullName() : "Dr. " + doc.getId())
                    .specialty(doc.getSpecialty())
                    .departmentName(doc.getDepartment() != null ? doc.getDepartment().getName() : "N/A")
                    .totalAppointments(totalApts)
                    .completedConsultations(completedApts)
                    .averageConsultationMinutes(round(avgDuration, 1))
                    .medianConsultationMinutes(round(medianDuration, 1))
                    .build());
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<DepartmentWorkloadResponse> getDepartmentWorkload(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);

        List<Department> departments = departmentRepository.findAll();
        List<DepartmentWorkloadResponse> responses = new ArrayList<>();

        for (Department dept : departments) {
            List<Appointment> deptAppointments = appointmentRepository.findAppointmentsForAnalytics(
                    rangeStart, rangeEnd, null, dept.getId()
            );

            long total = deptAppointments.size();
            long completed = deptAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
            long cancelled = deptAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
            long activeDoctors = doctorRepository.findByDepartmentId(dept.getId()).size();

            responses.add(DepartmentWorkloadResponse.builder()
                    .departmentId(dept.getId())
                    .departmentName(dept.getName())
                    .totalAppointments(total)
                    .completedAppointments(completed)
                    .cancelledAppointments(cancelled)
                    .activeDoctorsCount(activeDoctors)
                    .build());
        }

        return responses;
    }

    private double calculatePercentile(List<Integer> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        if (sortedValues.size() == 1) return sortedValues.get(0);
        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sortedValues.get(lower);
        return sortedValues.get(lower) + (index - lower) * (sortedValues.get(upper) - sortedValues.get(lower));
    }

    private double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
