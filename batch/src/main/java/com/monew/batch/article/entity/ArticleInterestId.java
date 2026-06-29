package com.monew.batch.article.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * article_interests 테이블의 복합키입니다.
 * article_id와 interest_id 조합 하나가 "이 기사는 이 관심사에 속한다"는 매핑 1건을 의미합니다.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ArticleInterestId implements Serializable {

  @Column(name = "article_id")
  private UUID articleId;
  @Column(name = "interest_id")
  private UUID interestId;

  /**
   * ArticleInterest를 새로 만들 때 두 FK 값으로 복합키를 구성합니다.
   */
  public ArticleInterestId(UUID articleId, UUID interestId) {
    this.articleId = articleId;
    this.interestId = interestId;
  }
}
