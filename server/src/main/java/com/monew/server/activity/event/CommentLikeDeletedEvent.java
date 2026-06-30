package com.monew.server.activity.event;

import java.util.UUID;

public record CommentLikeDeletedEvent(
    UUID userId,
    UUID commentLikeId,
    UUID commentId,
    long commentLikeCount
) {
}