package com.mediqueue.audit.service;

import com.mediqueue.audit.entity.ClinicalAuditEvent;
import com.mediqueue.audit.repository.ClinicalAuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClinicalAuditService {

    private final ClinicalAuditEventRepository auditEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(
            Long actorUserId,
            String actorRole,
            String action,
            String resourceType,
            Long resourceId,
            Long patientId,
            String outcome,
            String safeReason
    ) {
        try {
            ClinicalAuditEvent event = ClinicalAuditEvent.builder()
                    .actorUserId(actorUserId)
                    .actorRole(actorRole)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .patientId(patientId)
                    .outcome(outcome)
                    .safeReason(safeReason)
                    .eventTimestamp(Instant.now())
                    .build();
            auditEventRepository.save(event);
            log.info("Clinical audit recorded: action={}, resourceType={}, resourceId={}, outcome={}",
                    action, resourceType, resourceId, outcome);
        } catch (Exception e) {
            log.error("Failed to persist clinical audit event for action {}: {}", action, e.getMessage());
        }
    }
}
