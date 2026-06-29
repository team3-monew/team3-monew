package com.monew.server.activity.event;

import java.util.UUID;

public record ArticleViewCountUpdatedEvent(
    UUID articleId,
    long articleViewCount
) {
}