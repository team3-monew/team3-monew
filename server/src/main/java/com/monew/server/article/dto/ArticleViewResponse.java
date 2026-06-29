package com.monew.server.article.dto;

import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleView;
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
        String title,
        LocalDateTime publishDate,
        String summary,
        long commentCount,
        long viewCount
) {

    public static ArticleViewResponse from(ArticleView articleView) {
        Article article = articleView.getArticle();

        return new ArticleViewResponse(
                articleView.getId(),
                articleView.getUser().getId(),
                articleView.getCreatedAt(),
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishDate(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount()
        );
    }
}