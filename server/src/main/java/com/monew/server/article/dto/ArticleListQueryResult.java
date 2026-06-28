package com.monew.server.article.dto;

import java.time.LocalDateTime;

public record ArticleListQueryResult(
        ArticleResponse article,
        LocalDateTime createdAt
) {
}