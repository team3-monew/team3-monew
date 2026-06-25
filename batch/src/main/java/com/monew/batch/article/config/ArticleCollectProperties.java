package com.monew.batch.article.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 기사 수집 배치에서 사용하는 설정값을 application.yml 또는 환경변수에서 바인딩합니다.
 * 수집 개수, 재시도 횟수/간격, 스케줄 cron 값을 코드 하드코딩 없이 조정하기 위한 클래스입니다.
 */
@ConfigurationProperties(prefix = "monew.article-collect")
public record ArticleCollectProperties(
    int naverDisplay,
    int retryMaxAttempts,
    long retryInitialDelayMillis,
    long retryMaxDelayMillis,
    String cron
) {
}
