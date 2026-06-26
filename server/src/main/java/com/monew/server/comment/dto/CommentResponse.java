package com.monew.server.comment.dto;

import com.monew.server.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    String content,
    String nickname,
    long likeCount,
    LocalDateTime createdAt
) {

  // 엔티티를 DTO로 편하게 변환하기 위한 팩토리 메서드
  public static CommentResponse from(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getContent(),
        comment.getUser().getNickname(),
        comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }
}