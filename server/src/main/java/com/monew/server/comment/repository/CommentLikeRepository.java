package com.monew.server.comment.repository;

import com.monew.server.comment.entity.CommentLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

//이미 좋아요 눌렀는지 확인
  Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

//좋아요 취소
  void deleteByCommentIdAndUserId(UUID commentId, UUID userId);

  // 좋아요 여부만 빠르게 확인 (Boolean 반환)
  boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);


  // [변경]
  // 존재 확인(exists) 후 저장(save)하던 기존 방식은
  // 두 요청이 거의 동시에 들어오면 둘 다 확인을 통과해버릴 수 있음(TOCTOU).
  // INSERT 자체를 원자적으로 시도하고, UNIQUE(comment_id, user_id) 충돌 시
  // 예외 없이 조용히 무시(ON CONFLICT DO NOTHING) → 반환값(영향 row 수)으로 성공 여부 판단
  @Modifying(flushAutomatically = true)
  @Query(value = """
      insert into comment_likes (id, comment_id, user_id, created_at)
      values (:id, :commentId, :userId, CURRENT_TIMESTAMP)
      on conflict (comment_id, user_id) do nothing
      """, nativeQuery = true)
  int insertIgnore(
      @Param("id") UUID id,
      @Param("commentId") UUID commentId,
      @Param("userId") UUID userId
  );
}