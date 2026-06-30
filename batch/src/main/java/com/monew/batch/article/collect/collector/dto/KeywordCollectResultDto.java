package com.monew.batch.article.collect.collector.dto;

import java.util.List;

/**
 * 키워드 기반 API 호출 한 번의 결과를 담는 DTO입니다.
 */
public record KeywordCollectResultDto(
    List<CollectedArticleDto> articles,   // 기사 목록
    int requestCount,   // 요청 수
    int successCount,   // 요청 성공 수
    int failureCount    // 요청 실패 수
) {
}
