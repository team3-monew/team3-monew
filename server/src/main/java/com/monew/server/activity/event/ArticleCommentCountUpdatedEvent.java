package com.monew.server.activity.event;

import java.util.UUID;

public record ArticleCommentCountUpdatedEvent(
    UUID articleId,
    long articleCommentCount
) {
}