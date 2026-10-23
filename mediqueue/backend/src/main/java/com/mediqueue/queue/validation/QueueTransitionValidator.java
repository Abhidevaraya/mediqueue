package com.mediqueue.queue.validation;

import com.mediqueue.exception.BusinessException;
import com.mediqueue.queue.enums.QueueStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class QueueTransitionValidator {

    private static final Map<QueueStatus, Set<QueueStatus>> ALLOWED_TRANSITIONS = Map.of(
            QueueStatus.WAITING, Set.of(QueueStatus.CALLED, QueueStatus.SKIPPED, QueueStatus.CANCELLED),
            QueueStatus.CALLED, Set.of(QueueStatus.IN_PROGRESS, QueueStatus.WAITING, QueueStatus.SKIPPED, QueueStatus.CANCELLED),
            QueueStatus.IN_PROGRESS, Set.of(QueueStatus.COMPLETED),
            QueueStatus.SKIPPED, Set.of(QueueStatus.WAITING, QueueStatus.CANCELLED),
            QueueStatus.COMPLETED, Set.of(),
            QueueStatus.CANCELLED, Set.of()
    );

    public void validateTransition(QueueStatus currentStatus, QueueStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        Set<QueueStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException(String.format(
                    "Invalid queue transition from %s to %s. Allowed target statuses: %s",
                    currentStatus, targetStatus, allowed
            ));
        }
    }
}
