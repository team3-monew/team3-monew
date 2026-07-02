package com.monew.server.article.dto;

import com.monew.server.article.entity.Article;
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

    public static ArticleResponse from(Article article, boolean viewedByMe) {
        return new ArticleResponse(
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishDate(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount(),
                viewedByMe
        );
    }
}