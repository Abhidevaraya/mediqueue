package com.mediqueue.prediction.service;

import com.mediqueue.prediction.dto.WaitTimePredictionResponse;
import com.mediqueue.prediction.entity.PredictionLog;
import com.mediqueue.prediction.repository.PredictionLogRepository;
import com.mediqueue.queue.entity.DoctorQueueSession;
import com.mediqueue.queue.entity.QueueEntry;
import com.mediqueue.queue.enums.QueueStatus;
import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import com.mediqueue.queue.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.mediqueue.consultation.entity.ConsultationRecord;
import com.mediqueue.consultation.repository.ConsultationRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingTimePredictionService {

    private static final int MIN_HISTORICAL_SAMPLES = 3;
    private static final int DEFAULT_FALLBACK_MINUTES = 15;

    private final QueueEntryRepository queueEntryRepository;
    private final DoctorQueueSessionRepository doctorQueueSessionRepository;
    private final PredictionLogRepository predictionLogRepository;

    @Autowired(required = false)
    private ConsultationRecordRepository consultationRecordRepository;

    @Transactional(readOnly = true)
    public WaitTimePredictionResponse calculateWaitTime(QueueEntry entry) {
        if (entry.getStatus() == QueueStatus.IN_PROGRESS || entry.getStatus() == QueueStatus.CALLED) {
            return WaitTimePredictionResponse.builder()
                    .queueEntryId(entry.getId())
                    .tokenNumber(entry.getTokenNumber())
                    .eligiblePatientsAhead(0)
                    .estimatedWaitMinutes(0)
                    .lowerBoundMinutes(0)
                    .upperBoundMinutes(0)
                    .predictionMethod("DIRECT_CALL")
                    .usesFallback(false)
                    .queuePaused(false)
                    .lastUpdatedAt(Instant.now())
                    .message(entry.getStatus() == QueueStatus.IN_PROGRESS
                            ? "Consultation currently in progress."
                            : "Token called. Please proceed to consultation room.")
                    .build();
        }

        if (entry.getStatus() == QueueStatus.COMPLETED || entry.getStatus() == QueueStatus.CANCELLED) {
            return WaitTimePredictionResponse.builder()
                    .queueEntryId(entry.getId())
                    .tokenNumber(entry.getTokenNumber())
                    .eligiblePatientsAhead(0)
                    .estimatedWaitMinutes(0)
                    .lowerBoundMinutes(0)
                    .upperBoundMinutes(0)
                    .predictionMethod("TERMINAL_STATE")
                    .usesFallback(false)
                    .queuePaused(false)
                    .lastUpdatedAt(Instant.now())
                    .message("Queue entry has ended with status: " + entry.getStatus())
                    .build();
        }

        // 1. Calculate eligible patients ahead
        int patientsAhead = queueEntryRepository.countWaitingAhead(
                entry.getDoctor().getId(),
                entry.getQueueDate(),
                entry.getCheckInTimestamp()
        );

        // 2. Fetch completed consultation durations for this doctor
        List<Integer> durations = new ArrayList<>();

        if (consultationRecordRepository != null) {
            List<ConsultationRecord> recentClinical = consultationRecordRepository.findRecentCompletedConsultations(
                    entry.getDoctor().getId(),
                    PageRequest.of(0, 20)
            );
            for (ConsultationRecord cr : recentClinical) {
                if (cr.getActualStartTimestamp() != null && cr.getActualEndTimestamp() != null) {
                    long minutes = Duration.between(cr.getActualStartTimestamp(), cr.getActualEndTimestamp()).toMinutes();
                    if (minutes > 0 && minutes <= 180) {
                        durations.add((int) minutes);
                    }
                }
            }
        }

        if (durations.isEmpty()) {
            List<QueueEntry> recentCompleted = queueEntryRepository.findRecentCompletedConsultations(
                    entry.getDoctor().getId(),
                    PageRequest.of(0, 20)
            );
            for (QueueEntry qe : recentCompleted) {
                if (qe.getConsultationStartTimestamp() != null && qe.getConsultationEndTimestamp() != null) {
                    long minutes = Duration.between(qe.getConsultationStartTimestamp(), qe.getConsultationEndTimestamp()).toMinutes();
                    if (minutes > 0 && minutes <= 180) {
                        durations.add((int) minutes);
                    }
                }
            }
        }

        int baselineDurationMinutes;
        String predictionMethod;
        boolean usesFallback;

        if (durations.size() >= MIN_HISTORICAL_SAMPLES) {
            Collections.sort(durations);
            baselineDurationMinutes = durations.get(durations.size() / 2);
            predictionMethod = "DOCTOR_HISTORY_MEDIAN";
            usesFallback = false;
        } else {
            baselineDurationMinutes = entry.getAppointment().getSlotDurationMinutes() != null && entry.getAppointment().getSlotDurationMinutes() > 0
                    ? entry.getAppointment().getSlotDurationMinutes()
                    : DEFAULT_FALLBACK_MINUTES;
            predictionMethod = "SCHEDULE_SLOT_DURATION_FALLBACK";
            usesFallback = true;
        }

        // 3. Check currently in-progress consultation
        Optional<QueueEntry> inProgressEntry = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                entry.getDoctor().getId(),
                entry.getQueueDate(),
                QueueStatus.IN_PROGRESS
        );

        int remainingMinutesCurrent = 0;
        if (inProgressEntry.isPresent() && inProgressEntry.get().getConsultationStartTimestamp() != null) {
            long elapsed = Duration.between(inProgressEntry.get().getConsultationStartTimestamp(), Instant.now()).toMinutes();
            remainingMinutesCurrent = Math.max(0, baselineDurationMinutes - (int) elapsed);
        }

        // Also check if a patient is currently CALLED
        Optional<QueueEntry> calledEntry = queueEntryRepository.findFirstByDoctorIdAndQueueDateAndStatus(
                entry.getDoctor().getId(),
                entry.getQueueDate(),
                QueueStatus.CALLED
        );
        int calledOverhead = calledEntry.isPresent() ? baselineDurationMinutes : 0;

        int estimatedWait = remainingMinutesCurrent + calledOverhead + (patientsAhead * baselineDurationMinutes);
        int lowerBound = Math.max(0, (int) Math.round(estimatedWait * 0.75));
        int upperBound = (int) Math.round(estimatedWait * 1.35) + 5;

        // 4. Check Queue session pause state
        Optional<DoctorQueueSession> sessionOpt = doctorQueueSessionRepository.findByDoctorIdAndQueueDate(
                entry.getDoctor().getId(),
                entry.getQueueDate()
        );
        boolean isPaused = sessionOpt.map(DoctorQueueSession::getIsPaused).orElse(false);

        String message;
        if (isPaused) {
            String reason = sessionOpt.map(DoctorQueueSession::getPauseReason).orElse("Doctor temporarily unavailable");
            message = "Doctor queue is currently paused (" + reason + "). Estimated waiting time may be prolonged.";
        } else if (patientsAhead == 0 && inProgressEntry.isEmpty() && calledEntry.isEmpty()) {
            message = "You are next in queue! Please be ready.";
        } else {
            message = String.format("Estimated waiting time is ~%d mins with %d patient(s) ahead.", estimatedWait, patientsAhead);
        }

        return WaitTimePredictionResponse.builder()
                .queueEntryId(entry.getId())
                .tokenNumber(entry.getTokenNumber())
                .eligiblePatientsAhead(patientsAhead)
                .estimatedWaitMinutes(estimatedWait)
                .lowerBoundMinutes(lowerBound)
                .upperBoundMinutes(upperBound)
                .predictionMethod(predictionMethod)
                .usesFallback(usesFallback)
                .queuePaused(isPaused)
                .lastUpdatedAt(Instant.now())
                .message(message)
                .build();
    }

    @Transactional
    public void recordPredictionLog(QueueEntry entry, WaitTimePredictionResponse prediction) {
        try {
            PredictionLog logEntry = PredictionLog.builder()
                    .queueEntry(entry)
                    .doctorId(entry.getDoctor().getId())
                    .queueDate(entry.getQueueDate())
                    .eligiblePatientsAhead(prediction.getEligiblePatientsAhead())
                    .predictedWaitMinutes(prediction.getEstimatedWaitMinutes())
                    .predictionMethod(prediction.getPredictionMethod())
                    .usesFallback(prediction.getUsesFallback())
                    .build();
            predictionLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to persist prediction log for entry {}: {}", entry.getId(), e.getMessage());
        }
    }
}
