package com.monew.server.article.dto;

import com.monew.server.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleViewResponse(
        UUID id,
        UUID viewedBy,
        LocalDateTime createdAt,
        UUID articleId,
        ArticleSource source,
        String sourceUrl,
        String articleTitle,
        LocalDateTime articlePublishedDate,
        String articleSummary,
        long articleCommentCount,
        long articleViewCount
) {
}