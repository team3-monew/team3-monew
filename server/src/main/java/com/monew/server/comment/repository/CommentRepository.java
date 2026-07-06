package com.monew.server.comment.repository;

import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.repository.querydsl.CommentQueryRepository; // [신규] import
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// [변경] CommentQueryRepository 상속 추가
public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentQueryRepository {

    long countByArticleIdAndDeletedAtIsNull(UUID articleId);

    // [삭제] findCommentsByArticleValueCursor (JPQL, CASE WHEN 혼합) → CommentQueryRepositoryImpl로 이동
    // [삭제] findCommentsByArticleLikeCursor (JPQL, CASE WHEN 혼합) → CommentQueryRepositoryImpl로 이동

    List<Comment> findTop10ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<Comment> findByIdAndDeletedAtIsNull(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :commentId")
    int increaseLikeCount(@Param("commentId") UUID commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount - 1 where c.id = :commentId and c.likeCount > 0")
    int decreaseLikeCount(@Param("commentId") UUID commentId);
}