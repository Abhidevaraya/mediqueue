package com.mediqueue.notification.service;

import com.mediqueue.common.PageResponse;
import com.mediqueue.exception.ResourceNotFoundException;
import com.mediqueue.notification.dto.NotificationResponse;
import com.mediqueue.notification.dto.UnreadCountResponse;
import com.mediqueue.notification.entity.Notification;
import com.mediqueue.notification.enums.NotificationType;
import com.mediqueue.notification.repository.NotificationRepository;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mediqueue.notification.enums.NotificationDeliveryStatus;
import com.mediqueue.notification.provider.ExternalNotificationProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ExternalNotificationProvider externalNotificationProvider;

    @Transactional
    public Notification createNotification(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            Long appointmentId,
            Long queueEntryId
    ) {
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for notification with ID: " + recipientUserId));

        Notification notification = Notification.builder()
                .recipientUser(recipient)
                .notificationType(type)
                .title(title)
                .message(message)
                .appointmentId(appointmentId)
                .queueEntryId(queueEntryId)
                .isRead(false)
                .deliveryStatus(NotificationDeliveryStatus.PENDING)
                .retryCount(0)
                .build();

        // Attempt dispatch via external notification provider
        try {
            boolean sent = externalNotificationProvider != null && externalNotificationProvider.send(notification);
            notification.setLastAttemptAt(Instant.now());
            if (sent) {
                notification.setDeliveryStatus(NotificationDeliveryStatus.SENT);
            } else {
                notification.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
                notification.setFailureReason("Provider rejected dispatch");
            }
        } catch (Exception e) {
            notification.setLastAttemptAt(Instant.now());
            notification.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
            notification.setFailureReason("Provider error: " + e.getMessage());
            log.warn("External notification dispatch failed for user {}: {}", recipient.getEmail(), e.getMessage());
        }

        Notification saved = notificationRepository.save(notification);
        log.info("Persisted notification ID {} for user {}: {}", saved.getId(), recipient.getEmail(), title);

        // Try to dispatch live to user private STOMP queue if connected
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/notifications",
                    NotificationResponse.from(saved)
            );
        } catch (Exception e) {
            log.debug("Live WebSocket notification delivery skipped or failed for {}: {}", recipient.getEmail(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    public int retryFailedNotifications() {
        List<Notification> failedList = notificationRepository.findTop20ByDeliveryStatusOrderByCreatedAtAsc(NotificationDeliveryStatus.FAILED);
        int successCount = 0;
        for (Notification notif : failedList) {
            if (notif.getRetryCount() >= 3) {
                // Max retries reached; do not retry further
                continue;
            }
            notif.setRetryCount(notif.getRetryCount() + 1);
            notif.setLastAttemptAt(Instant.now());
            try {
                boolean sent = externalNotificationProvider != null && externalNotificationProvider.send(notif);
                if (sent) {
                    notif.setDeliveryStatus(NotificationDeliveryStatus.SENT);
                    notif.setFailureReason(null);
                    successCount++;
                } else {
                    notif.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
                    notif.setFailureReason("Retry attempt rejected by provider");
                }
            } catch (Exception e) {
                notif.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
                notif.setFailureReason("Retry error: " + e.getMessage());
            }
            notificationRepository.save(notif);
        }
        return successCount;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(AuthenticatedUser principal, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(principal.getId(), pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getMyUnreadCount(AuthenticatedUser principal) {
        int count = notificationRepository.countByRecipientUserIdAndIsReadFalse(principal.getId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(AuthenticatedUser principal, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipientUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Access denied: You can only mark your own notifications as read");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }

        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(AuthenticatedUser principal) {
        notificationRepository.markAllAsReadForUser(principal.getId(), Instant.now());
    }
}
