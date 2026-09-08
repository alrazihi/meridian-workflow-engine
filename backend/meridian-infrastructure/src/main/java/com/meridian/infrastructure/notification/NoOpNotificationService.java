package com.meridian.infrastructure.notification;

import com.meridian.application.port.outbound.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NoOpNotificationService implements NotificationService {
    @Override
    public void notifyTaskAssigned(String assignee, String taskId, String action) {
    }
}
