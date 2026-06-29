package com.monew.batch.article.collect.entity;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.entity.ArticleSource;
import com.monew.batch.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기사 수집 Step들이 외부 API/RSS에서 가져온 저장 후보 기사를 임시로 쌓아두는 staging Entity입니다.
 * 같은 배치 실행(jobExecutionId) 안에서는 sourceUrl 기준으로 한 번만 저장되도록 해서,
 * Naver와 RSS에서 같은 원문 URL이 들어와도 뒤 저장 Step에서 중복 처리하기 쉽도록 합니다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(
    name = "article_collect_staging",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_article_collect_staging_job_url",
        columnNames = {"job_execution_id", "source_url"}
    )
)
public class ArticleCollectStaging extends BaseCreatedEntity {

  @Id
  private UUID id;

  @Column(name = "job_execution_id", nullable = false)
  private Long jobExecutionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ArticleSource source;

  @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
  private String sourceUrl;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(name = "publish_date", nullable = false)
  private LocalDateTime publishDate;

  @Column(columnDefinition = "TEXT")
  private String summary;

  public ArticleCollectStaging(Long jobExecutionId, CollectedArticleDto article) {
    this.jobExecutionId = jobExecutionId;
    this.source = article.source();
    this.sourceUrl = article.sourceUrl();
    this.title = article.title();
    this.publishDate = article.publishDate();
    this.summary = article.summary();
  }

  public CollectedArticleDto toCollectedArticleDto() {
    return new CollectedArticleDto(source, sourceUrl, title, publishDate, summary);
  }

  /**
   * staging row가 처음 저장되기 직전에 ID와 생성 시각을 채웁니다.
   */
  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }
}
