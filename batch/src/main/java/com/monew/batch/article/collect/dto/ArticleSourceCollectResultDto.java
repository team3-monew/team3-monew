package com.monew.batch.article.collect.dto;

public record ArticleSourceCollectResultDto(
    String source,    // 기사 출처
    int stagedCount,    // stage된 기사 개수
    int savedCount,   // 저장된 기사 개수
    int duplicateSkippedCount,    // 중복 개수
    int invalidSkippedCount   //유효하지 않은 기사 개수
) {
}
