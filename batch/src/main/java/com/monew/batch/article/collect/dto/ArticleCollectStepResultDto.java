package com.monew.batch.article.collect.dto;

import com.monew.batch.article.entity.ArticleSource;

/**
 * Naver/RSS 수집 Step 하나의 실행 결과를 담는 DTO입니다.
 */
public record ArticleCollectStepResultDto(
    ArticleSource source,   // 기사 출처
    int requestCount,   // 요청 수
    int successCount,   //성공 수
    int failureCount,   //실패 수
    int collectedCount,   // 기사 수집 개수
    int matchedCount,   // 관심사 키워드와 매칭된 기사 개수
    int stagedCount,    // staging에 저장한 기사 개수
    int duplicateSkippedCount   // 중복으로 스킵한 기사 개수
) {

  public static ArticleCollectStepResultDto empty(ArticleSource source) {
    return new ArticleCollectStepResultDto(source, 0, 0, 0, 0, 0, 0, 0);
  }
}
