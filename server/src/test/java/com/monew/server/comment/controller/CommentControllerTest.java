package com.monew.server.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.service.CommentService;
import com.monew.server.support.ControllerTestSupport;
import java.time.LocalDateTime;
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
    then(commentService).should(org.mockito.Mockito.never())
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
}