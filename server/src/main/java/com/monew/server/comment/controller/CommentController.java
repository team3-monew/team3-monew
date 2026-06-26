package com.monew.server.comment.controller;

import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.dto.CommentSliceResult;
import com.monew.server.comment.service.CommentService;
import com.monew.server.common.response.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  //뉴스 기사 별 댓글 목록 조회 (커서 페이지네이션 API)
  @GetMapping("/articles/{articleId}/comments")
  public ResponseEntity<CursorPageResponse<CommentResponse>> getCommentsByArticle(
      @PathVariable UUID articleId,
      @RequestParam(defaultValue = "CREATED_AT") String sortBy,
      @RequestParam(required = false) LocalDateTime lastCreatedAt,
      @RequestParam(required = false) Long lastLikeCount,
      @RequestParam(defaultValue = "10") int size
  ) {

    CommentSliceResult result = commentService
        .getCommentsByArticleCursor(articleId, sortBy, lastCreatedAt, lastLikeCount, size);

    List<CommentResponse> commentResponses = result.content().stream()
        .map(CommentResponse::from)
        .toList();

    CursorPageResponse<CommentResponse> response = new CursorPageResponse<>(
        commentResponses,
        result.nextCursor(),
        result.nextAfter(),
        result.size(),
        0,
        result.hasNext()
    );

    return ResponseEntity.ok(response);
  }


  //댓글 등록 API
  @PostMapping("/articles/{articleId}/comments")
  public ResponseEntity<UUID> createComment(
      @PathVariable UUID articleId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId,
      @RequestBody String content
  ) {
    UUID commentId = commentService.createComment(articleId, userId, content);
    return ResponseEntity.ok(commentId);
  }


  //댓글 수정 API
  @PatchMapping("/comments/{commentId}")
  public ResponseEntity<Void> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId,
      @RequestBody String content
  ) {
    commentService.updateComment(commentId, userId, content);
    return ResponseEntity.noContent().build();
  }


 //댓글 삭제 API(논리삭제)
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID commentId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId
  ) {
    commentService.deleteComment(commentId, userId);
    return ResponseEntity.noContent().build();
  }


 //댓글 좋아요, 좋아요 취소 (토글 API)
  @PostMapping("/comments/{commentId}/like")
  public ResponseEntity<Void> toggleCommentLike(
      @PathVariable UUID commentId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId
  ) {
    commentService.toggleCommentLike(commentId, userId);
    return ResponseEntity.noContent().build();
  }
}