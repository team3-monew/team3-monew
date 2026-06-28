package com.monew.server.article.dto;

import com.monew.server.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        ArticleSource source,
        String sourceUrl,
        String title,
        LocalDateTime publishDate,
        String summary,
        long commentCount,
        long viewCount,
        boolean viewedByMe
) {
}