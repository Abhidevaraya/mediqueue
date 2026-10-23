package com.mediqueue.notification.dto;

import com.mediqueue.notification.entity.Notification;
import com.mediqueue.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType notificationType;
    private String title;
    private String message;
    private Boolean isRead;
    private Instant readAt;
    private Long appointmentId;
    private Long queueEntryId;
    private String deliveryStatus;
    private Instant createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .appointmentId(notification.getAppointmentId())
                .queueEntryId(notification.getQueueEntryId())
                .deliveryStatus(notification.getDeliveryStatus() != null ? notification.getDeliveryStatus().name() : null)
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
