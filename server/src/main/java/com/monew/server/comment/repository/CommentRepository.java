package com.monew.server.comment.repository;

import com.monew.server.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    long countByArticleIdAndDeletedAtIsNull(UUID articleId);

    //날짜 순
    // comments 테이블에는 is_deleted 컬럼이 없고 deleted_at만 있으므로
    // 삭제되지 않은 댓글은 deletedAt이 null인 조건으로 조회하는 방향으로 수정했습니다
    @Query("SELECT c FROM Comment c " +
            "WHERE c.article.id = :articleId " +
            "AND c.deletedAt IS NULL " +
            "AND (:lastCreatedAt IS NULL OR (" +
            "    (:direction = 'ASC' AND (c.createdAt > :lastCreatedAt OR (c.createdAt = :lastCreatedAt AND c.id > :lastId))) OR " +
            "    (:direction = 'DESC' AND (c.createdAt < :lastCreatedAt OR (c.createdAt = :lastCreatedAt AND c.id < :lastId))))) " +
            "ORDER BY CASE WHEN :direction = 'ASC' THEN c.createdAt END ASC, " +
            "         CASE WHEN :direction = 'DESC' THEN c.createdAt END DESC, " +
            "         CASE WHEN :direction = 'ASC' THEN c.id END ASC, " +
            "         CASE WHEN :direction = 'DESC' THEN c.id END DESC")
    List<Comment> findCommentsByArticleValueCursor(
            @Param("articleId") UUID articleId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") UUID lastId,
            @Param("direction") String direction,
            Pageable pageable
    );

    //좋아요 순
    // comments 테이블에는 is_deleted 컬럼이 없고 deleted_at만 있으므로
    // 삭제되지 않은 댓글은 deletedAt이 null인 조건으로 조회하는 방향으로 수정했습니다
    @Query("SELECT c FROM Comment c " +
            "WHERE c.article.id = :articleId " +
            "AND c.deletedAt IS NULL " +
            "AND (:lastLikeCount IS NULL OR (" +
            "    (:direction = 'ASC' AND (c.likeCount > :lastLikeCount OR (c.likeCount = :lastLikeCount AND c.id > :lastId))) OR " +
            "    (:direction = 'DESC' AND (c.likeCount < :lastLikeCount OR (c.likeCount = :lastLikeCount AND c.id < :lastId))))) " +
            "ORDER BY CASE WHEN :direction = 'ASC' THEN c.likeCount END ASC, " +
            "         CASE WHEN :direction = 'DESC' THEN c.likeCount END DESC, " +
            "         CASE WHEN :direction = 'ASC' THEN c.id END ASC, " +
            "         CASE WHEN :direction = 'DESC' THEN c.id END DESC")
    List<Comment> findCommentsByArticleLikeCursor(
            @Param("articleId") UUID articleId,
            @Param("lastLikeCount") Long lastLikeCount,
            @Param("lastId") UUID lastId, // 중복 방지용 ID 커서 추가
            @Param("direction") String direction,
            Pageable pageable
    );

    //활동 내역 조회용(특정 유저가 작성한 최신 댓글 최대 10개)
    List<Comment> findTop10ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<Comment> findByIdAndDeletedAtIsNull(UUID id);


    // [변경]
    // 좋아요 수 원자적 증감 쿼리 추가
    // clearAutomatically=true: 벌크 UPDATE는 영속성 컨텍스트를 거치지 않고 DB에 직접 나가므로,
    // 캐시된 Comment 엔티티가 stale해지는 걸 막기 위해 컨텍스트를 비운다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :commentId")
    int increaseLikeCount(@Param("commentId") UUID commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount - 1 where c.id = :commentId and c.likeCount > 0")
    int decreaseLikeCount(@Param("commentId") UUID commentId);
}