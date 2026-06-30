package com.monew.server.activity.event;

import java.util.UUID;

public record CommentUpdatedEvent(
    UUID userId,
    UUID commentId,
    String content
) {
}
