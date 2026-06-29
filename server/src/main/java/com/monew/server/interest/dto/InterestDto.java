package com.monew.server.interest.dto;

import com.monew.server.interest.entity.Interest;

import java.util.List;
import java.util.UUID;

public record InterestDto(
        UUID id,
        String name,
        List<String> keywords,
        long subscriberCount,
        boolean subscribedByMe
) {
    public static InterestDto of(
            Interest interest,
            List<String> keywords,
            boolean subscribedByMe
    ) {
        return new InterestDto(
                interest.getId(),
                interest.getName(),
                keywords,
                interest.getSubscriberCount(),
                subscribedByMe
        );
    }
}
