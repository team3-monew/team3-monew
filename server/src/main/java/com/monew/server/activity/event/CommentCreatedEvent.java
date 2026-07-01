package com.monew.server.activity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentCreatedEvent(
    UUID userId,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    String userNickname,
    String content,
    long likeCount,
    LocalDateTime createdAt
) {
}
