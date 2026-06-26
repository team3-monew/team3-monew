package com.monew.batch.article.collect.collector;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.entity.ArticleSource;
import java.util.List;

public interface KeywordBasedArticleCollector {

  ArticleSource getSource();

  /**
   * 전달받은 키워드로 외부 API를 조회하고, 저장 가능한 기사 DTO 목록으로 변환합니다.
   */
  List<CollectedArticleDto> collectByKeyword(String keyword, int limit);

}
