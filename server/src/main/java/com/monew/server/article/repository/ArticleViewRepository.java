package com.monew.server.article.repository;

import com.monew.server.article.entity.ArticleView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

    Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);
}