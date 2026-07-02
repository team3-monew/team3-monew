package com.monew.server.notification.repository.querydsl;

import com.monew.server.notification.dto.NotificationResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationQueryRepository {

    List<NotificationResponse> findUnreadNotifications(
            UUID userId,
            String cursor,
            LocalDateTime after,
            int limit
    );

    long countUnreadNotifications(UUID userId);
}