package com.monew.server.article.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "article_backups")
public class ArticleBackup {

  @Id
  private UUID id;
  @Column(name = "backup_date", nullable = false, unique = true)
  private LocalDate backupDate;
  @Column(name = "s3_bucket", nullable = false)
  private String s3Bucket;
  @Column(name = "s3_object_key", nullable = false, columnDefinition = "TEXT")
  private String s3ObjectKey;
  @Column(name = "article_count", nullable = false)
  private long articleCount;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ArticleBackupStatus status;
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
      if (id == null) {
          id = UUID.randomUUID();
      }
      if (createdAt == null) {
          createdAt = LocalDateTime.now();
      }
  }
}
