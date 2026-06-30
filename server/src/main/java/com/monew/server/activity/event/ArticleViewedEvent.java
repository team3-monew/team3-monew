package com.monew.server.activity.event;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewedEvent(
    UUID userId,
    UUID articleViewId,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    long articleCommentCount,
    long articleViewCount,
    Instant createdAt
) {
}