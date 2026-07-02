package com.monew.batch.article.collect.dto;

import java.util.List;

/**
 * staging 데이터를 articles에 저장하고, 저장된 기사와 관심사를 연결한 결과입니다.
 * 두 작업은 데이터 정합성이 이어져 있으므로 하나의 Step에서 함께 처리합니다.
 */
public record ArticleSaveAndInterestLinkStepResultDto(
    int stagedCount,    // staging 테이블에 있던 전체 수집 기사 수
    int savedCount,   // articles 테이블에 새로 저장된 기사 수
    int duplicateSkippedCount,    // articles 테이블에 존재해서 저장하지 않은 중복 기사 수
    int invalidSkippedCount,    // 저장할 수 없는 비정상 기사
    int articleInterestLinkedCount,    // Article과 Interest가 연결된 개수
    List<ArticleSourceCollectResultDto> sourceResults   // 위 결과 source 별로 나눈 목록들
) {
}
