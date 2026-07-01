package com.monew.server.article.entity;

import com.monew.server.article.dto.ArticleBackupDto;
import com.monew.server.common.entity.BaseTimeEntity;
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

@Getter
@NoArgsConstructor
@Entity
@Table(name = "articles")
public class Article extends BaseTimeEntity {

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

  private Article(
      ArticleSource source,
      String sourceUrl,
      String title,
      LocalDateTime publishDate,
      String summary,
      long commentCount,
      long viewCount,
      LocalDateTime deletedAt
  ) {
    this.id = UUID.randomUUID();
    this.source = source;
    this.sourceUrl = sourceUrl;
    this.title = title;
    this.publishDate = publishDate;
    this.summary = summary;
    this.commentCount = commentCount;
    this.viewCount = viewCount;
    this.deletedAt = deletedAt;
  }

  public static Article fromBackup(ArticleBackupDto dto) {
    return new Article(
        dto.source(),
        dto.sourceUrl(),
        dto.title(),
        dto.publishDate(),
        dto.summary(),
        dto.commentCount(),
        dto.viewCount(),
        dto.deletedAt()
    );
  }

  public void increaseViewCount() {
    this.viewCount++;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }
}