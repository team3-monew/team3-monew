package com.monew.batch.article.collect.collector;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.entity.ArticleSource;
import java.util.List;

/**
 * RSS 기반 기사 수집기가 공통으로 따라야 하는 인터페이스입니다.
 */
public interface FeedBasedArticleCollector {

  ArticleSource getSource();

  /**
   * RSS를 수집하고, 기사 목록뿐 아니라 요청 성공/실패 횟수까지 함께 반환합니다.
   */
  RssCollectResultDto collectLatestResult(int limit);

  /**
   * 기존 코드에서 기사 목록만 필요할 때 사용할 수 있는 편의 메서드입니다.
   */
  default List<CollectedArticleDto> collectLatest(int limit) {
    return collectLatestResult(limit).articles();
  }
}
