package com.mediqueue.notification.provider;

import com.mediqueue.notification.entity.Notification;

public interface ExternalNotificationProvider {

    boolean send(Notification notification);

    String getProviderName();
}
