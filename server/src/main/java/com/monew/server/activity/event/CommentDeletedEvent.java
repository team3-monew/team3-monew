package com.monew.server.activity.event;

import java.util.UUID;

public record CommentDeletedEvent(
    UUID userId,
    UUID commentId
) {
}
