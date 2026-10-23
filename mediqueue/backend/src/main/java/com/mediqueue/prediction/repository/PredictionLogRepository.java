package com.mediqueue.prediction.repository;

import com.mediqueue.prediction.entity.PredictionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionLogRepository extends JpaRepository<PredictionLog, Long> {

    List<PredictionLog> findByQueueEntryId(Long queueEntryId);

    List<PredictionLog> findByQueueDateBetween(java.time.LocalDate from, java.time.LocalDate to);

    List<PredictionLog> findByQueueDateBetweenAndDoctorId(java.time.LocalDate from, java.time.LocalDate to, Long doctorId);

    List<PredictionLog> findByQueueDate(java.time.LocalDate date);
}
