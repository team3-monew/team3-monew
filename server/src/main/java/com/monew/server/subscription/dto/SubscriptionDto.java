package com.monew.server.subscription.dto;

import com.monew.server.interest.entity.Interest;
import com.monew.server.subscription.entity.Subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
        UUID id,
        UUID interestId,
        String interestName,
        List<String> interestKeywords,
        long interestSubscriberCount,
        LocalDateTime createdAt
) {
    public static SubscriptionDto of(
            Subscription subscription,
            Interest interest,
            List<String> interestKeywords
    ) {
        return new SubscriptionDto(
                subscription.getId(),
                interest.getId(),
                interest.getName(),
                interestKeywords,
                interest.getSubscriberCount(),
                subscription.getCreatedAt()
        );
    }
}
