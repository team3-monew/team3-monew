package com.monew.batch.article.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Naver Open API 호출에 필요한 baseUrl과 인증 정보를 설정에서 주입받습니다.
 * 민감한 clientId/clientSecret은 환경변수로 넣는 것을 전제로 합니다.
 */
@ConfigurationProperties(prefix = "monew.api.naver")
public record NaverApiProperties(
    String baseUrl,
    String clientId,
    String clientSecret
) {
}
