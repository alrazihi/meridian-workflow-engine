package com.meridian.application.port.outbound;

import com.meridian.domain.model.DocumentEvent;

public interface EventPublisher {
    void publish(DocumentEvent event);
}
