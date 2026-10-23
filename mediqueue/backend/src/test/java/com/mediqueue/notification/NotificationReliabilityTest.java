package com.mediqueue.notification;

import com.mediqueue.notification.entity.Notification;
import com.mediqueue.notification.enums.NotificationDeliveryStatus;
import com.mediqueue.notification.enums.NotificationType;
import com.mediqueue.notification.provider.ExternalNotificationProvider;
import com.mediqueue.notification.repository.NotificationRepository;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.UserRole;
import com.mediqueue.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationReliabilityTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ExternalNotificationProvider externalNotificationProvider;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = User.builder().id(10L).email("patient@example.com").role(UserRole.PATIENT).build();
    }

    @Test
    @DisplayName("Notification creation sets deliveryStatus = SENT when provider succeeds")
    void createNotification_providerSuccess_setsStatusSent() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(recipient));
        when(externalNotificationProvider.send(any(Notification.class))).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(
                10L, NotificationType.FOLLOW_UP_REMINDER, "Follow-up", "Test message", 100L, null
        );

        assertThat(result.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(result.getLastAttemptAt()).isNotNull();
        assertThat(result.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Notification creation sets deliveryStatus = FAILED when provider throws exception")
    void createNotification_providerFailure_setsStatusFailed() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(recipient));
        when(externalNotificationProvider.send(any(Notification.class))).thenThrow(new RuntimeException("Gateway timeout"));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(
                10L, NotificationType.FOLLOW_UP_REMINDER, "Follow-up", "Test message", 100L, null
        );

        assertThat(result.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(result.getFailureReason()).contains("Gateway timeout");
    }

    @Test
    @DisplayName("retryFailedNotifications retries failed items up to max 3 attempts")
    void retryFailedNotifications_retriesUpToMaxAttempts() {
        Notification failed1 = Notification.builder()
                .id(1L)
                .deliveryStatus(NotificationDeliveryStatus.FAILED)
                .retryCount(0)
                .build();
        Notification failed2 = Notification.builder()
                .id(2L)
                .deliveryStatus(NotificationDeliveryStatus.FAILED)
                .retryCount(3) // Already maxed out
                .build();

        when(notificationRepository.findTop20ByDeliveryStatusOrderByCreatedAtAsc(NotificationDeliveryStatus.FAILED))
                .thenReturn(List.of(failed1, failed2));
        when(externalNotificationProvider.send(failed1)).thenReturn(true);

        int recovered = notificationService.retryFailedNotifications();

        assertThat(recovered).isEqualTo(1);
        assertThat(failed1.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(failed1.getRetryCount()).isEqualTo(1);
        // failed2 should remain untouched because retryCount was already 3
        assertThat(failed2.getRetryCount()).isEqualTo(3);
    }
}
