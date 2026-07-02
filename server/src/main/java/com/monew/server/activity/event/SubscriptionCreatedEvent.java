package com.monew.server.activity.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubscriptionCreatedEvent(
    UUID userId,
    UUID subscriptionId,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    long interestSubscriberCount,
    LocalDateTime createdAt
) {
}
