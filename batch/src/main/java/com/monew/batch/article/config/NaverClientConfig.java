package com.monew.batch.article.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Naver API 전용 RestClient Bean을 구성합니다.
 * baseUrl과 인증 헤더를 여기에서 한 번만 세팅해 수집기 코드를 단순하게 유지합니다.
 */
@Configuration
public class NaverClientConfig {

  /**
   * Naver 뉴스 검색 API를 호출할 때 사용할 HTTP client입니다.
   */
  @Bean
  RestClient naverRestClient(NaverApiProperties properties) {
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader("X-Naver-Client-Id", properties.clientId())
        .defaultHeader("X-Naver-Client-Secret", properties.clientSecret())
        .build();
  }
}
