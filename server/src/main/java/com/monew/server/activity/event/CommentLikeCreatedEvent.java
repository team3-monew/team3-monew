package com.monew.server.activity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentLikeCreatedEvent(
    UUID userId,
    UUID commentLikeId,
    LocalDateTime createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    long commentLikeCount,
    LocalDateTime commentCreatedAt
) {
}
