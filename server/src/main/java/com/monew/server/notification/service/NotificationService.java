package com.monew.server.notification.service;

import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.notification.dto.NotificationResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationService {

    CursorPageResponse<NotificationResponse> findUnreadNotifications(
            UUID userId,
            String cursor,
            LocalDateTime after,
            int limit
    );

    void confirm(UUID userId, UUID notificationId);

    void confirmAll(UUID userId);

    void createCommentLikeNotification(
            UUID receiverUserId,
            UUID likerUserId,
            UUID commentId,
            String likerNickname
    );
}