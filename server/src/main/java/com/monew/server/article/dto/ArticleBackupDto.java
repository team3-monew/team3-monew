package com.monew.server.article.dto;

import com.monew.server.article.entity.ArticleSource;
import java.time.LocalDateTime;

public record ArticleBackupDto(
    ArticleSource source,   // 기사 출처
    String sourceUrl,   // 기사 원본 링크
    String title,   // 기사 제목
    LocalDateTime publishDate,    // 기사 발행일
    String summary,   // 기사 요약
    long commentCount,    // 댓글수
    long viewCount,   // 조회수
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
}
