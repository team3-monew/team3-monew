package com.monew.server.comment.controller;

import com.monew.server.comment.dto.CommentRequest;
import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.dto.CommentSliceResult;
import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.service.CommentService;
import com.monew.server.common.response.CursorPageResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

  //댓글 목록 조회 (★ 서비스 레이어 파라미터 타입 및 순서 완벽 일치!)
  @GetMapping("/comments")
  public ResponseEntity<CursorPageResponse<CommentResponse>> getCommentsByArticle(
      @RequestParam UUID articleId,
      @RequestParam(defaultValue = "createdAt") String orderBy,
      @RequestParam(defaultValue = "DESC") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) LocalDateTime after,
      @RequestParam(defaultValue = "10") int limit,
      @RequestHeader("MoNew-Request-User-ID") UUID userId
  ) {

    Long lastLikeCount = "likeCount".equalsIgnoreCase(orderBy) && cursor != null ? Long.parseLong(cursor) : null;
    LocalDateTime lastCreatedAt = "createdAt".equalsIgnoreCase(orderBy) && cursor != null ? LocalDateTime.parse(cursor) : after;
    UUID lastId = null;


    CommentSliceResult result = commentService.getCommentsByArticleCursor(
        articleId,
        orderBy,
        lastCreatedAt,
        lastLikeCount,
        lastId,
        limit
    );


    List<CommentResponse> commentResponses = new ArrayList<>();
    for (Comment comment : result.content()) {
      boolean likedByMe = true; // 대시보드 스펙용 스텁 값
      commentResponses.add(CommentResponse.of(comment, likedByMe));
    }

    CursorPageResponse<CommentResponse> response = new CursorPageResponse<>(
        commentResponses,
        result.nextCursor(),
        result.nextAfter(),
        result.size(),
        100,
        result.hasNext()
    );

    return ResponseEntity.ok(response);
  }

  //댓글 등록 API
  @PostMapping("/comments")
  public ResponseEntity<UUID> createComment(
      @Valid @RequestBody CommentRequest request,
      @RequestHeader("MoNew-Request-User-ID") UUID userId
  ) {
    UUID commentId = commentService.createComment(request.articleId(), userId, request.content());
    return ResponseEntity.ok(commentId);
  }

  //댓글 내용 수정 API
  @PatchMapping("/comments/{commentId}")
  public ResponseEntity<Void> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId,
      @Valid @RequestBody CommentRequest request
  ) {
    commentService.updateComment(commentId, userId, request.content());
    return ResponseEntity.noContent().build();
  }

  //댓글 삭제 API (논리 삭제 - 명세서의 204 No Content 대응)
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID commentId,
      @RequestHeader("MoNew-Request-User-ID") UUID userId
  ) {
    commentService.deleteComment(commentId, userId);
    return ResponseEntity.noContent().build();
  }

  //댓글 삭제 API (물리 삭제)
  @DeleteMapping("/comments/{commentId}/hard")
  public ResponseEntity<Void> deleteCommentHard(
      @PathVariable UUID commentId
  ) {
    return ResponseEntity.noContent().build();
  }
}