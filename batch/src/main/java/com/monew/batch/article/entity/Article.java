package com.monew.batch.article.entity;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수집한 기사를 저장하는 articles 테이블 Entity입니다.
 * sourceUrl에 unique 제약이 있어 같은 원문 기사가 중복 저장되지 않도록 합니다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "articles")
public class Article {

  @Id
  private UUID id;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ArticleSource source;
  @Column(name = "source_url", nullable = false, unique = true, columnDefinition = "TEXT")
  private String sourceUrl;
  @Column(nullable = false, length = 500)
  private String title;
  @Column(name = "publish_date", nullable = false)
  private LocalDateTime publishDate;
  @Column(columnDefinition = "TEXT")
  private String summary;
  @Column(name = "comment_count", nullable = false)
  private long commentCount;
  @Column(name = "view_count", nullable = false)
  private long viewCount;
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * 외부 API에서 변환된 CollectedArticle 값을 실제 저장 Entity로 만들 때 사용합니다.
   */
  public Article(ArticleSource source, String sourceUrl, String title, LocalDateTime publishDate,
      String summary) {
    this.source = source;
    this.sourceUrl = sourceUrl;
    this.title = title;
    this.publishDate = publishDate;
    this.summary = summary;
    this.commentCount = 0L;
    this.viewCount = 0L;
  }

  public static Article from(CollectedArticleDto collectedArticleDto){
    return new Article(
        collectedArticleDto.source(),
        collectedArticleDto.sourceUrl(),
        collectedArticleDto.title(),
        collectedArticleDto.publishDate(),
        collectedArticleDto.summary()
    );
  }

  /**
   * 저장 직전에 id와 생성/수정 시간을 채웁니다.
   */
  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }
}
