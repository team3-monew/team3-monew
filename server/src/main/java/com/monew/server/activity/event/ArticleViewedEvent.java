package com.monew.server.activity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleViewedEvent(
    UUID userId,
    UUID articleViewId,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    LocalDateTime articlePublishedDate,
    String articleSummary,
    long articleCommentCount,
    long articleViewCount,
    LocalDateTime createdAt
) {
}
