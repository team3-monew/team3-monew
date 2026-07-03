package com.monew.server.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentLikeResponse;
import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.dto.CommentUpdateRequest;
import com.monew.server.comment.service.CommentService;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.support.ControllerTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CommentController.class)
class CommentControllerTest extends ControllerTestSupport {

  @MockitoBean
  private CommentService commentService;

  @Test
  @DisplayName("댓글 등록 성공 - 유효한 요청이면 201과 댓글 정보를 응답한다")
  void createComment_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(articleId, "댓글 내용");

    CommentResponse response = new CommentResponse(
        commentId, articleId, userId, "테스터", "댓글 내용",
        0L, false, LocalDateTime.now());

    given(commentService.createCommentResponse(any(CommentCreateRequest.class), any(UUID.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(commentId.toString()))
        .andExpect(jsonPath("$.content").value("댓글 내용"));
  }

  @Test
  @DisplayName("댓글 등록 실패 - 내용이 공백이면 400을 응답한다")
  void createComment_fail_blankContent() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(articleId, "   "); // 공백만

    // when & then
    mockMvc.perform(post("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    // Bean Validation에서 막혔으므로 Service는 호출조차 되면 안 된다
    then(commentService).should(never())
        .createCommentResponse(any(), any());
  }

  @Test
  @DisplayName("댓글 등록 실패 - 내용이 500자를 초과하면 400을 응답한다")
  void createComment_fail_contentTooLong() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    String tooLong = "a".repeat(501);
    CommentCreateRequest request = new CommentCreateRequest(articleId, tooLong);

    // when & then
    mockMvc.perform(post("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("댓글 수정 성공 - 유효한 요청이면 200과 수정된 댓글 정보를 응답한다")
  void updateComment_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");
    CommentResponse response = new CommentResponse(
        commentId, UUID.randomUUID(), userId, "테스터", "수정된 내용", 0L, false, LocalDateTime.now());

    given(commentService.updateCommentResponse(eq(commentId), eq(userId), any(CommentUpdateRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("수정된 내용"));
  }

  @Test
  @DisplayName("댓글 수정 실패 - 내용이 공백이면 400을 응답한다")
  void updateComment_fail_blankContent() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("   ");

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    then(commentService).should(never()).updateCommentResponse(any(), any(), any());
  }


  @Test
  @DisplayName("댓글 삭제 성공 - 유효한 요청이면 204를 응답한다")
  void deleteComment_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    then(commentService).should().deleteComment(commentId, userId);
  }

  @Test
  @DisplayName("댓글 삭제 실패 - Monew-Request-User-ID 헤더가 없으면 400을 응답한다")
  void deleteComment_fail_missingUserIdHeader() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isUnauthorized());

    then(commentService).should(never()).deleteComment(any(), any());
  }



  @Test
  @DisplayName("좋아요 등록 성공 - 유효한 요청이면 200과 좋아요 정보를 응답한다")
  void addLike_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    CommentLikeResponse response = new CommentLikeResponse(
        UUID.randomUUID(), userId, LocalDateTime.now(), commentId, articleId,
        UUID.randomUUID(), "작성자", "댓글 내용", 1L, LocalDateTime.now());

    given(commentService.addLikeAndGetResponse(commentId, userId)).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.commentLikeCount").value(1));
  }

  @Test
  @DisplayName("좋아요 취소 성공 - 유효한 요청이면 204를 응답한다")
  void removeLike_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    then(commentService).should().removeLike(commentId, userId);
  }


  @Test
  @DisplayName("댓글 목록 조회 성공 - orderBy를 생략하면 기본값(createdAt)이 적용된다")
  void getCommentsByArticle_success_defaultOrderBy() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    CursorPageResponse<CommentResponse> response =
        new CursorPageResponse<>(List.of(), null, null, 0, 0, false);

    given(commentService.getComments(
        eq(articleId), eq("createdAt"), eq("DESC"), any(), any(), eq(10), eq(userId)))
        .willReturn(response);

    // when & then
    // orderBy 파라미터를 생략 — @RequestParam(defaultValue = "createdAt")이 적용되어야 함
    mockMvc.perform(get("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .param("articleId", articleId.toString())
            .param("direction", "DESC")
            .param("limit", "10"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("댓글 목록 조회 실패 - 지원하지 않는 정렬 기준이면 400을 응답한다")
  void getCommentsByArticle_fail_invalidOrderBy() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    given(commentService.getComments(
        eq(articleId), eq("asdf"), any(), any(), any(), anyInt(), eq(userId)))
        .willThrow(new BaseException(CommonErrorCode.INVALID_REQUEST));

    // when & then
    mockMvc.perform(get("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .param("articleId", articleId.toString())
            .param("orderBy", "asdf")
            .param("direction", "DESC")
            .param("limit", "10"))
        .andExpect(status().isBadRequest());
  }


  @Test
  @DisplayName("댓글 목록 조회 실패 - articleId가 없으면 400을 응답한다")
  void getCommentsByArticle_fail_missingArticleId() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    mockMvc.perform(get("/api/comments")
            .header("Monew-Request-User-ID", userId.toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("limit", "10"))
        .andExpect(status().isBadRequest());
  }
}