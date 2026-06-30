package com.monew.batch.article.collect.dto;

/**
 * 기사 수집 staging 데이터를 정리한 결과입니다.
 */
public record ArticleCollectStagingCleanupResultDto(
    long deletedCount
) {
}
