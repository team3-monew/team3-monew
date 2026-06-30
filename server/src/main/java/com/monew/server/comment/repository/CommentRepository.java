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

 long countByArticleIdAndDeletedAtIsNull(UUID articleId);

 //날짜 순
 @Query("SELECT c FROM Comment c " +
     "WHERE c.article.id = :articleId " +
     "AND c.isDeleted = false " +
     "AND (:lastCreatedAt IS NULL OR c.createdAt > :lastCreatedAt OR (c.createdAt = :lastCreatedAt AND c.id > :lastId)) " +
     "ORDER BY c.createdAt ASC, c.id ASC")
 List<Comment> findCommentsByArticleValueCursor(
     @Param("articleId") UUID articleId,
     @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
     @Param("lastId") UUID lastId,
     String direction,
     Pageable pageable
 );

  //좋아요 순
  @Query("SELECT c FROM Comment c " +
      "WHERE c.article.id = :articleId " +
      "AND c.isDeleted = false " +
      "AND (:lastLikeCount IS NULL OR c.likeCount < :lastLikeCount OR (c.likeCount = :lastLikeCount AND c.id < :lastId)) " +
      "ORDER BY c.likeCount DESC, c.id DESC")
  List<Comment> findCommentsByArticleLikeCursor(
      @Param("articleId") UUID articleId,
      @Param("lastLikeCount") Long lastLikeCount,
      @Param("lastId") UUID lastId, // 중복 방지용 ID 커서 추가
      String direction,
      Pageable pageable
  );

 //활동 내역 조회용(특정 유저가 작성한 최신 댓글 최대 10개)
  List<Comment> findTop10ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
}