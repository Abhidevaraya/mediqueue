package com.mediqueue.notification;

import com.mediqueue.notification.dto.NotificationResponse;
import com.mediqueue.notification.dto.UnreadCountResponse;
import com.mediqueue.notification.entity.Notification;
import com.mediqueue.notification.enums.NotificationType;
import com.mediqueue.notification.repository.NotificationRepository;
import com.mediqueue.notification.service.NotificationService;
import com.mediqueue.security.AuthenticatedUser;
import com.mediqueue.user.entity.User;
import com.mediqueue.user.enums.AccountStatus;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("alice@example.com")
                .fullName("Alice")
                .role(UserRole.PATIENT)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        principal = new AuthenticatedUser(user);
    }

    @Test
    @DisplayName("Create notification saves entity and dispatches to STOMP")
    void createNotification_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(99L);
            return n;
        });

        Notification created = notificationService.createNotification(
                1L,
                NotificationType.TOKEN_CALLED,
                "Token Called",
                "Your token has been called",
                10L,
                20L
        );

        assertNotNull(created);
        assertEquals(99L, created.getId());
        assertEquals("Token Called", created.getTitle());
        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("alice@example.com"), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Mark as read succeeds for recipient user")
    void markAsRead_success() {
        Notification notification = Notification.builder()
                .id(99L)
                .recipientUser(user)
                .isRead(false)
                .build();
        when(notificationRepository.findById(99L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse resp = notificationService.markAsRead(principal, 99L);

        assertTrue(resp.getIsRead());
        assertNotNull(resp.getReadAt());
    }

    @Test
    @DisplayName("Mark as read throws AccessDeniedException for different user")
    void markAsRead_accessDeniedForOtherUser() {
        User otherUser = User.builder().id(2L).email("other@example.com").build();
        Notification notification = Notification.builder()
                .id(99L)
                .recipientUser(otherUser)
                .isRead(false)
                .build();
        when(notificationRepository.findById(99L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () ->
                notificationService.markAsRead(principal, 99L)
        );
    }

    @Test
    @DisplayName("Get unread count returns accurate number")
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByRecipientUserIdAndIsReadFalse(1L)).thenReturn(4);

        UnreadCountResponse resp = notificationService.getMyUnreadCount(principal);
        assertEquals(4, resp.getUnreadCount());
    }
}
