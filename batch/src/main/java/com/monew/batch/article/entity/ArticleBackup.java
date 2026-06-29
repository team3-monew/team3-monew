package com.monew.batch.article.entity;

import com.monew.batch.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "article_backups")
public class ArticleBackup extends BaseCreatedEntity {

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

  /**
   * 새 백업 이력을 RUNNING 상태로 생성합니다.
   */
  public static ArticleBackup running(LocalDate backupDate, String s3Bucket, String s3ObjectKey) {
    ArticleBackup articleBackup = new ArticleBackup();
    articleBackup.backupDate = backupDate;
    articleBackup.s3Bucket = s3Bucket;
    articleBackup.s3ObjectKey = s3ObjectKey;
    articleBackup.articleCount = 0L;
    articleBackup.status = ArticleBackupStatus.RUNNING;
    return articleBackup;
  }

  /**
   * 기존 날짜의 백업을 재실행할 때 이력을 RUNNING 상태로 초기화합니다.
   */
  public void start(String s3Bucket, String s3ObjectKey) {
    this.s3Bucket = s3Bucket;
    this.s3ObjectKey = s3ObjectKey;
    this.articleCount = 0L;
    this.status = ArticleBackupStatus.RUNNING;
  }

  /**
   * 업로드 성공 후 백업 기사 수와 SUCCESS 상태를 기록합니다.
   */
  public void succeed(long articleCount) {
    this.articleCount = articleCount;
    this.status = ArticleBackupStatus.SUCCESS;
  }

  /**
   * 백업 처리 중 실패가 발생했음을 기록합니다.
   */
  public void fail() {
    this.status = ArticleBackupStatus.FAILED;
  }

  @PrePersist
  void prePersist() {
      if (id == null) {
          id = UUID.randomUUID();
      }
  }
}
