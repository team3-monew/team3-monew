package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.common.response.CursorPageResponse;
import java.util.List;
import java.util.UUID;

public interface ArticleService {

    CursorPageResponse<ArticleResponse> findArticles(ArticleSearchCondition condition, UUID userId);

    ArticleResponse findArticle(UUID articleId, UUID userId);

    ArticleViewResponse registerArticleView(UUID articleId, UUID userId);

    void softDelete(UUID articleId);

    void hardDelete(UUID articleId);

    List<ArticleSource> findSources();
}