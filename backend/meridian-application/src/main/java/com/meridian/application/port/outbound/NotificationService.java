package com.meridian.application.port.outbound;

public interface NotificationService {
    void notifyTaskAssigned(String assignee, String taskId, String action);
}
