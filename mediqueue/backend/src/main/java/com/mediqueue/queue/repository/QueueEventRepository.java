package com.mediqueue.queue.repository;

import com.mediqueue.queue.entity.QueueEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QueueEventRepository extends JpaRepository<QueueEvent, Long> {

    List<QueueEvent> findByQueueEntryIdOrderByEventTimestampDesc(Long queueEntryId);

    Page<QueueEvent> findByDoctorIdAndQueueDateOrderByEventTimestampDesc(Long doctorId, LocalDate queueDate, Pageable pageable);
}
