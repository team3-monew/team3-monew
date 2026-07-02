package com.monew.server.comment.controller;

import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentLikeResponse;
import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.dto.CommentUpdateRequest;
import com.monew.server.comment.service.CommentService;
import com.monew.server.common.response.CursorPageResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;


  //댓글 목록 조회
  @GetMapping("/comments")
  public ResponseEntity<CursorPageResponse<CommentResponse>> getCommentsByArticle(
      @RequestParam UUID articleId,
      @RequestParam(defaultValue = "createdAt") String orderBy,
      @RequestParam(defaultValue = "DESC") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) LocalDateTime after,
      @RequestParam(defaultValue = "10") int limit,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    return ResponseEntity
        .ok(commentService.getComments(articleId, orderBy, direction, cursor, after, limit, userId));
  }


  //댓글 등록 API
  @PostMapping("/comments")
  public ResponseEntity<CommentResponse> createComment(
      @Valid @RequestBody CommentCreateRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    return ResponseEntity.ok(commentService.createCommentResponse(request, userId));
  }


  //댓글 내용 수정 API
  @PatchMapping("/comments/{commentId}")
  public ResponseEntity<CommentResponse> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @Valid @RequestBody CommentUpdateRequest request
  ) {
    return ResponseEntity.ok(commentService.updateCommentResponse(commentId, userId, request));
  }


  //댓글 삭제 API (논리 삭제)
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.deleteComment(commentId, userId);
    return ResponseEntity.noContent().build();
  }


  //댓글 삭제 API (물리 삭제) -> 삭제
  //향후 테스트나 운영상 필요할 경우 테스트 코드 내에서 Repository를 직접 호출하여 처리할 예정.


  // 좋아요, 좋아요 취소 API
  // 좋아요 추가 (POST) // 스웨거 명세에 맞게 이름만 바꿨습니다
  @PostMapping("/comments/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeResponse> addLike(
          @PathVariable UUID commentId,
          @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    return ResponseEntity.ok(commentService.addLikeAndGetResponse(commentId, userId));
  }

  // 좋아요 취소 (DELETE)
  @DeleteMapping("/comments/{commentId}/comment-likes")
  public ResponseEntity<Void> removeLike(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.removeLike(commentId, userId);
    return ResponseEntity.noContent().build();
  }
}