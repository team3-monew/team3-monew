package com.monew.batch.article.collect.collector;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.KeywordCollectResultDto;
import com.monew.batch.article.entity.ArticleSource;
import java.util.List;

/**
 * 관심사 키워드를 검색어로 받아 기사를 수집하는 수집기 인터페이스입니다.
 */
public interface KeywordBasedArticleCollector {

  ArticleSource getSource();

  default KeywordCollectResultDto collectByKeywordResult(String keyword, int limit) {
    List<CollectedArticleDto> articles = collectByKeyword(keyword, limit);
    return new KeywordCollectResultDto(articles, 1, 1, 0);
  }

  List<CollectedArticleDto> collectByKeyword(String keyword, int limit);
}
