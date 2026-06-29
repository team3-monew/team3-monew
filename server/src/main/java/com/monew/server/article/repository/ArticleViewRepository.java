package com.monew.server.article.repository;

import com.monew.server.article.entity.ArticleView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

    Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);

    boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);

    @Modifying(flushAutomatically = true)
    @Query(
            value = """
                    insert into article_views (id, article_id, user_id, created_at)
                    values (:id, :articleId, :userId, CURRENT_TIMESTAMP)
                    on conflict (article_id, user_id) do nothing
                    """,
            nativeQuery = true
    )
    int insertIgnore(
            @Param("id") UUID id,
            @Param("articleId") UUID articleId,
            @Param("userId") UUID userId
    );
}