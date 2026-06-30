package com.monew.server.article.repository;

import com.monew.server.article.entity.Article;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findByIdAndDeletedAtIsNull(UUID articleId);

    boolean existsByIdAndDeletedAtIsNull(UUID articleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
      update Article a
      set a.viewCount = a.viewCount + 1,
          a.updatedAt = CURRENT_TIMESTAMP
      where a.id = :articleId
        and a.deletedAt is null
      """)
    int increaseViewCount(@Param("articleId") UUID articleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      select a
      from Article a
      where a.id = :articleId
        and a.deletedAt is null
      """)
    Optional<Article> findActiveByIdForUpdate(@Param("articleId") UUID articleId);
}