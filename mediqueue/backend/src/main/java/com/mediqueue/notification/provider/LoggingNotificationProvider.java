package com.mediqueue.notification.provider;

import com.mediqueue.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingNotificationProvider implements ExternalNotificationProvider {

    @Override
    public boolean send(Notification notification) {
        log.info("[NOTIFICATION DISPATCH] Provider: LocalDelivery | Recipient: {} | Type: {} | Title: {}",
                notification.getRecipientUser().getEmail(),
                notification.getNotificationType(),
                notification.getTitle());
        return true;
    }

    @Override
    public String getProviderName() {
        return "LOCAL_LOGGING_PROVIDER";
    }
}
