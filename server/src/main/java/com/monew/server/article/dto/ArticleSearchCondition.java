package com.monew.server.article.dto;

import com.monew.server.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArticleSearchCondition(
        String keyword,
        UUID interestId,
        List<ArticleSource> sourceIn,
        LocalDateTime publishDateFrom,
        LocalDateTime publishDateTo,
        String orderBy,
        String direction,
        String cursor,
        LocalDateTime after,
        int limit
) {
}