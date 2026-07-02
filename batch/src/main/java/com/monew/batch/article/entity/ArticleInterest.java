package com.monew.batch.article.entity;

import com.monew.batch.common.entity.BaseCreatedEntity;
import com.monew.batch.interest.entity.Interest;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기사와 관심사의 연결 관계를 저장하는 Entity입니다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "article_interests")
public class ArticleInterest extends BaseCreatedEntity {

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

  /**
   * Article과 Interest를 받아 복합키까지 함께 구성합니다.
   */
  public ArticleInterest(Article article, Interest interest) {
    this.article = article;
    this.interest = interest;
    this.id = new ArticleInterestId(article.getId(), interest.getId());
  }
}
