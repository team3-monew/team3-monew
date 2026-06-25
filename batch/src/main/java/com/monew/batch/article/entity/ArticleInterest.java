package com.monew.batch.article.entity;

import com.monew.batch.interest.entity.Interest;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기사와 관심사의 연결 관계를 저장하는 Entity입니다.
 * 수집된 기사의 제목/요약에 관심사 키워드가 포함되면 이 테이블에 매핑을 추가합니다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "article_interests")
public class ArticleInterest {

  @EmbeddedId
  private ArticleInterestId id;
  @MapsId("articleId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id")
  private Article article;
  @MapsId("interestId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "interest_id")
  private Interest interest;
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Article과 Interest를 받아 복합키까지 함께 구성합니다.
   */
  public ArticleInterest(Article article, Interest interest) {
    this.article = article;
    this.interest = interest;
    this.id = new ArticleInterestId(article.getId(), interest.getId());
  }

  /**
   * 새 매핑이 저장될 때 생성 시간을 자동으로 채웁니다.
   */
  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
