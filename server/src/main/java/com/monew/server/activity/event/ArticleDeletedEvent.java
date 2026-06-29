package com.monew.server.activity.event;

import java.util.UUID;

public record ArticleDeletedEvent(
    UUID articleId
) {
}
