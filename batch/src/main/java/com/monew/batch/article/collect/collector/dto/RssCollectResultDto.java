package com.monew.batch.article.collect.collector.dto;

import java.util.List;

// RSS 수집 결과를 담는 DTO
public record RssCollectResultDto(
    List<CollectedArticleDto> articles,  // 기사 목록
    int requestCount, // RSS URL 요청 횟수
    int successCount, // 요청 성공 횟수
    int failureCount  // 요청 실패 횟수
) {

  // RSS URL이 비어 있거나 수집할 대상이 없을 때 사용하는 빈 결과
  public static RssCollectResultDto empty() {
    return new RssCollectResultDto(List.of(), 0, 0, 0);
  }
}
