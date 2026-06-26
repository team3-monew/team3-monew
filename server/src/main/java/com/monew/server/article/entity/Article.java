package com.monew.server.article.entity;

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

  public void increaseViewCount() {
    this.viewCount++;
    this.updatedAt = LocalDateTime.now();
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

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