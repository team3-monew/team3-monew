package com.monew.server.comment.controller;

import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.dto.CommentUpdateRequest;
import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.repository.CommentLikeRepository;
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
  private final CommentLikeRepository commentLikeRepository;

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
    CursorPageResponse<CommentResponse> response = commentService.getComments(
        articleId, orderBy, direction, cursor, after, limit, userId
    );
    return ResponseEntity.ok(response);
  }

  //댓글 등록 API
  @PostMapping("/comments")
  public ResponseEntity<CommentResponse> createComment(
      @Valid @RequestBody CommentCreateRequest request,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    Comment comment = commentService.createComment(request, userId);
    boolean likedByMe = false;
    CommentResponse response = CommentResponse.of(comment, likedByMe);
    return ResponseEntity.ok(response);
  }

  //댓글 내용 수정 API
  @PatchMapping("/comments/{commentId}")
  public ResponseEntity<CommentResponse> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId,
      @Valid @RequestBody CommentUpdateRequest request
  ) {
    Comment updatedComment = commentService.updateComment(commentId, userId, request);
    boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    CommentResponse response = CommentResponse.of(updatedComment, likedByMe);
    return ResponseEntity.ok(response);
  }

  //댓글 삭제 API (논리 삭제 - 명세서의 204 No Content 대응)
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable UUID commentId,
      @RequestHeader("Monew-Request-User-ID") UUID userId
  ) {
    commentService.deleteComment(commentId, userId);
    return ResponseEntity.noContent().build();
  }

  //댓글 삭제(물리 삭제) -> 삭제
  //향후 테스트나 운영상 필요할 경우 테스트 코드 내에서 Repository를 직접 호출하여 처리할 예정.

}