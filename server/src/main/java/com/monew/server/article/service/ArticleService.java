package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.ArticleSource;
import java.util.List;
import java.util.UUID;

public interface ArticleService {

    ArticleViewResponse registerArticleView(UUID articleId, UUID userId);

    void softDelete(UUID articleId);

    List<ArticleSource> findSources();
}