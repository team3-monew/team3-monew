package com.monew.server.comment.repository;

import com.monew.server.comment.entity.CommentLike;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

//이미 좋아요 눌렀는지 확인
  Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

//좋아요 취소
  void deleteByCommentIdAndUserId(UUID commentId, UUID userId);
}