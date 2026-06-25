package com.monew.batch.article.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ArticleInterestId implements Serializable {

  @Column(name = "article_id")
  private UUID articleId;
  @Column(name = "interest_id")
  private UUID interestId;
}
