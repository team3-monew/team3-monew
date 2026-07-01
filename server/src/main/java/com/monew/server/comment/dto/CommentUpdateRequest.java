package com.monew.server.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
    @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
    @Size(max = 500, message = "댓글은 최대 500자까지 작성할 수 있습니다.")
    String content
) {}