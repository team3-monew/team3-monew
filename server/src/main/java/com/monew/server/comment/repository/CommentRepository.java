package com.monew.server.comment.repository;

import com.monew.server.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

 //날짜 순
  @Query("SELECT c FROM Comment c " +
      "WHERE c.article.id = :articleId " +
      "AND c.deletedAt IS NULL " +
      "AND (:lastCreatedAt IS NULL OR c.createdAt < :lastCreatedAt) " +
      "ORDER BY c.createdAt DESC, c.id DESC")
  List<Comment> findCommentsByArticleValueCursor(
      @Param("articleId") UUID articleId,
      @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
      Pageable pageable
  );

  //좋아요 순
  @Query("SELECT c FROM Comment c " +
      "WHERE c.article.id = :articleId " +
      "AND c.deletedAt IS NULL " +
      "AND (:lastLikeCount IS NULL OR c.likeCount < :lastLikeCount) " +
      "ORDER BY c.likeCount DESC, c.id DESC")
  List<Comment> findCommentsByArticleLikeCursor(
      @Param("articleId") UUID articleId,
      @Param("lastLikeCount") Long lastLikeCount,
      Pageable pageable
  );

 //활동 내역 조회용(특정 유저가 작성한 최신 댓글 최대 10개)
  List<Comment> findTop10ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
}