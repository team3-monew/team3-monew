package com.monew.server.article.repository.querydsl;

import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleSearchCondition;
import java.util.List;
import java.util.UUID;

public interface ArticleQueryRepository {

    List<ArticleListQueryResult> findArticles(
            ArticleSearchCondition condition,
            UUID userId
    );

    long countArticles(ArticleSearchCondition condition);
}