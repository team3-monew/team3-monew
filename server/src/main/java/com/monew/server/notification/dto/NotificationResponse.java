package com.monew.server.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        boolean confirmed,
        UUID userId,
        String content,
        String resourceType,
        UUID resourceId
) {
}