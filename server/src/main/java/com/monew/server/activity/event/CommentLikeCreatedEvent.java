package com.monew.server.activity.event;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeCreatedEvent(
    UUID userId,
    UUID commentLikeId,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    long commentLikeCount,
    Instant commentCreatedAt
) {
}