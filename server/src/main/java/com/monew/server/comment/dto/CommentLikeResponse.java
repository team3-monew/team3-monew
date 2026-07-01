package com.monew.server.comment.dto;

import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.entity.CommentLike;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentLikeResponse(
    UUID id,
    UUID likedBy,
    LocalDateTime createdAt,
    UUID commentId,
    UUID articleId,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    long commentLikeCount,
    LocalDateTime commentCreatedAt
) {
  public static CommentLikeResponse of(CommentLike commentLike, Comment comment) {
    return new CommentLikeResponse(
        commentLike.getId(),
        commentLike.getUser().getId(),
        commentLike.getCreatedAt() != null ? commentLike.getCreatedAt() : LocalDateTime.now(),
        comment.getId(),
        comment.getArticle().getId(),
        comment.getUser().getId(),
        comment.getUser().getNickname(),
        comment.getContent(),
        comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }
}