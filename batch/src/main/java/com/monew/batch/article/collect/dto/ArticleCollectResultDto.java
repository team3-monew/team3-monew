package com.monew.batch.article.collect.dto;


/**
 * 기사 수집 배치 1회 실행 결과를 요약하는 DTO입니다.
 * Naver/RSS 요청 수와 저장/중복/매핑 결과를 최종 로그로 남길 때 사용합니다.
 */
public record ArticleCollectResultDto(
    int naverRequestCount,
    int naverSuccessCount,
    int naverFailureCount,
    boolean naverRateLimitExceeded,
    int rssRequestCount,
    int rssSuccessCount,
    int rssFailureCount,
    int yeonhapRssRequestCount,
    int yeonhapRssSuccessCount,
    int yeonhapRssFailureCount,
    int collectedCount,
    int savedCount,
    int duplicateSkippedCount,
    int invalidSkippedCount,
    int articleInterestLinkedCount,
    int failedCount
) {
}
