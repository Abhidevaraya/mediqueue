package com.mediqueue.notification.repository;

import com.mediqueue.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    int countByRecipientUserIdAndIsReadFalse(Long recipientUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipientUser.id = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") Long userId, @Param("readAt") Instant readAt);

    Page<Notification> findByDeliveryStatusOrderByCreatedAtDesc(com.mediqueue.notification.enums.NotificationDeliveryStatus deliveryStatus, Pageable pageable);

    java.util.List<Notification> findTop20ByDeliveryStatusOrderByCreatedAtAsc(com.mediqueue.notification.enums.NotificationDeliveryStatus deliveryStatus);
}
