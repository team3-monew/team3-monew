package com.monew.server.comment.dto;

import com.monew.server.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;

public record CommentSliceResult(
    List<Comment> content,
    String nextCursor,
    LocalDateTime nextAfter,
    boolean hasNext,
    int size
) {}