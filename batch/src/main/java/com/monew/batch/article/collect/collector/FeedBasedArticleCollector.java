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

  RssCollectResultDto collectLatestResult(int limit);

  default List<CollectedArticleDto> collectLatest(int limit) {
    return collectLatestResult(limit).articles();
  }
}
