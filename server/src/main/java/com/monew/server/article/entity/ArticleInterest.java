package com.monew.server.article.entity;

import com.monew.server.common.entity.BaseCreatedEntity;
import com.monew.server.interest.entity.Interest;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}