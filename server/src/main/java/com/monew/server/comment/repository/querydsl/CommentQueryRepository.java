package com.monew.server.comment.repository.querydsl;

import com.monew.server.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

// CommentRepository의 기존 두 메서드 시그니처를 그대로 유지
// → CommentService 호출부는 수정할 필요 없음
public interface CommentQueryRepository {

  List<Comment> findCommentsByArticleValueCursor(
      UUID articleId,
      LocalDateTime lastCreatedAt,
      UUID lastId,
      String direction,
      Pageable pageable
  );

  List<Comment> findCommentsByArticleLikeCursor(
          UUID articleId, Long lastLikeCount, LocalDateTime lastCreatedAt, UUID lastId,
          String direction, Pageable pageable);

}