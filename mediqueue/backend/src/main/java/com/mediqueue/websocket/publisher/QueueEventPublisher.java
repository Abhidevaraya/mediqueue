package com.mediqueue.websocket.publisher;

import com.mediqueue.queue.event.QueueDomainEvent;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.repository.UserRepository;
import com.mediqueue.websocket.event.QueueRealTimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueueEventCommitted(QueueDomainEvent event) {
        log.info("Publishing committed queue event: {} for doctor ID {}, token {}",
                event.getEventType(), event.getDoctorId(), event.getTokenNumber());

        QueueRealTimeMessage message = QueueRealTimeMessage.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .doctorId(event.getDoctorId())
                .queueDate(event.getQueueDate())
                .queueEntryId(event.getQueueEntryId())
                .tokenNumber(event.getTokenNumber())
                .currentStatus(event.getNewStatus())
                .isPaused(event.isPaused())
                .timestamp(event.getEventTimestamp())
                .message(event.getMessage())
                .build();

        // 1. Broadcast to doctor's queue channel
        String doctorTopic = "/topic/doctors/" + event.getDoctorId() + "/queue";
        try {
            messagingTemplate.convertAndSend(doctorTopic, message);
            log.debug("Broadcasted message to {}", doctorTopic);
        } catch (Exception e) {
            log.error("Failed to broadcast message to {}: {}", doctorTopic, e.getMessage());
        }

        // 2. Dispatch private update to specific patient if applicable
        if (event.getPatientUserId() != null) {
            userRepository.findById(event.getPatientUserId()).ifPresent(user -> {
                try {
                    messagingTemplate.convertAndSendToUser(
                            user.getEmail(),
                            "/queue/status",
                            message
                    );
                    log.debug("Sent private queue update to patient: {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send private queue message to {}: {}", user.getEmail(), e.getMessage());
                }
            });
        }
    }
}
