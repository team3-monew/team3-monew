package com.monew.server.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        LocalDateTime createdAt,
        @JsonProperty("updatedAt")
        LocalDateTime confirmedAt,
        boolean confirmed,
        UUID userId,
        String content,
        String resourceType,
        UUID resourceId
) {
}