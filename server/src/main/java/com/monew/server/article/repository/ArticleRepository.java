package com.monew.server.article.repository;

import com.monew.server.article.entity.Article;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
      update Article a
      set a.commentCount = a.commentCount + 1,
          a.updatedAt = CURRENT_TIMESTAMP
      where a.id = :articleId
        and a.deletedAt is null
      """)
    int increaseCommentCount(@Param("articleId") UUID articleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
      update Article a
      set a.commentCount = a.commentCount - 1,
          a.updatedAt = CURRENT_TIMESTAMP
      where a.id = :articleId
        and a.deletedAt is null
        and a.commentCount > 0
      """)
    int decreaseCommentCount(@Param("articleId") UUID articleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      select a
      from Article a
      where a.id = :articleId
        and a.deletedAt is null
      """)
    Optional<Article> findActiveByIdForUpdate(@Param("articleId") UUID articleId);

    @Query("select a.sourceUrl from Article a where a.sourceUrl in :sourceUrls")
    Set<String> findExistingSourceUrls(@Param("sourceUrls") Collection<String> sourceUrls);
}