package com.mediqueue.followup.repository;

import com.mediqueue.followup.entity.FollowUp;
import com.mediqueue.followup.enums.FollowUpReminderStatus;
import com.mediqueue.followup.enums.FollowUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    Optional<FollowUp> findByFollowUpReference(String followUpReference);

    Page<FollowUp> findByPatientIdOrderByRecommendedDateAsc(Long patientId, Pageable pageable);

    Page<FollowUp> findByDoctorIdOrderByRecommendedDateAsc(Long doctorId, Pageable pageable);

    List<FollowUp> findByReminderStatusAndRecommendedDateLessThanEqual(
            FollowUpReminderStatus reminderStatus,
            LocalDate date
    );
}
