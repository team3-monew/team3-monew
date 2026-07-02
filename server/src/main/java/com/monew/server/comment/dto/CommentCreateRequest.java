package com.monew.server.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentCreateRequest(
    @NotNull(message = "기사 ID는 필수입니다.")
    UUID articleId,

    @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
    @Size(max = 500, message = "댓글은 최대 500자까지 작성할 수 있습니다.")
    String content
) {}