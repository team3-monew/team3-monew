package com.monew.server.activity.event;

import java.util.UUID;

public record SubscriptionDeletedEvent(
    UUID userId,
    UUID subscriptionId
) {
}
