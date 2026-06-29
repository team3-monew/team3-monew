package com.monew.server.comment.dto;

import com.monew.server.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID articleId,
    UUID userId,
    String userNickname,
    String content,
    long likeCount,
    boolean likedByMe,
    LocalDateTime createdAt
) {

  // 엔티티를 DTO로 편하게 변환하기 위한 팩토리 메서드
  public static CommentResponse of (Comment comment, boolean likedByMe) {
    return new CommentResponse(
        comment.getId(),
        comment.getArticle().getId(),
        comment.getUser().getId(),
        comment.getUser().getNickname(),
        comment.getContent(),
        comment.getLikeCount(),
        likedByMe,
        comment.getCreatedAt()
    );
  }
}