package com.monew.batch.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Object event) {
        if (event == null) {
            return;
        }

        applicationEventPublisher.publishEvent(event);
    }
}