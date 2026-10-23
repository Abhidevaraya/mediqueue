package com.mediqueue.medicalrecord.repository;

import com.mediqueue.medicalrecord.entity.VisitSummaryRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitSummaryRevisionRepository extends JpaRepository<VisitSummaryRevision, Long> {

    List<VisitSummaryRevision> findByVisitSummaryIdOrderByRevisionNumberDesc(Long visitSummaryId);
}
