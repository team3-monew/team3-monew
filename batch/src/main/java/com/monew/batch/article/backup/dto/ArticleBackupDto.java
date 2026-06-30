package com.monew.batch.article.backup.dto;

import com.monew.batch.article.entity.Article;
import com.monew.batch.article.entity.ArticleSource;
import java.time.LocalDateTime;

/**
 * 기사 백업 JSON 에 저장할 데이터 구조입니다.
 */
public record ArticleBackupDto(
    ArticleSource source,   // 기사 출처
    String sourceUrl,
    String title,
    LocalDateTime publishDate,
    String summary,
    long commentCount,
    long viewCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {

  /**
   * Article Entity 를 백업 JSON 용 DTO 로 변환합니다.
   */
  public static ArticleBackupDto from(Article article) {
    return new ArticleBackupDto(
        article.getSource(),
        article.getSourceUrl(),
        article.getTitle(),
        article.getPublishDate(),
        article.getSummary(),
        article.getCommentCount(),
        article.getViewCount(),
        article.getCreatedAt(),
        article.getUpdatedAt(),
        article.getDeletedAt()
    );
  }
}
