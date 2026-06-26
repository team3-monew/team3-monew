package com.monew.batch.article.collect.collector;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.dto.NaverNewsItemResponseDto;
import com.monew.batch.article.collect.dto.NaverNewsResponseDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.HtmlUtils;

/**
 * 관심사 키워드로 Naver 뉴스 검색 Open API를 호출하는 수집기입니다.
 * RSS 수집보다 먼저 실행되며, 응답 item을 공통 DTO인 CollectedArticle로 변환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsCollector implements KeywordBasedArticleCollector {

  private static final String NEWS_SEARCH_PATH = "/v1/search/news.json";

  private final RestClient naverRestClient;
  private final ArticleCollectProperties properties;

  /**
   * 이 수집기가 저장할 기사 출처는 NAVER입니다.
   */
  @Override
  public ArticleSource getSource() {
    return ArticleSource.NAVER;
  }

  /**
   * 키워드 하나에 대해 Naver 검색 API를 호출하고, 응답 item 목록을 CollectedArticle 목록으로 바꿉니다.
   */
  @Override
  public List<CollectedArticleDto> collectByKeyword(String keyword, int limit) {
    int display = Math.min(limit, properties.naverDisplay());
    NaverNewsResponseDto response = requestWithRetry(keyword, display);

    if (response == null || response.items() == null) {
      return List.of();
    }

    return response.items().stream()
        .map(CollectedArticleDto::from)
        .filter(article -> article.sourceUrl() != null && !article.sourceUrl().isBlank())
        .toList();
  }

  /**
   * Naver API 호출이 일시적으로 실패하면 재시도합니다.
   * 최종 실패하면 null을 반환해서 전체 배치가 멈추지 않도록 합니다.
   */
  private NaverNewsResponseDto requestWithRetry(String keyword, int display) {
    int maxAttempts = Math.max(1, properties.retryMaxAttempts());

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        // 성공이면 뉴스 100개 가져옴
        return request(keyword, display);
      } catch (RestClientResponseException ex) {
        if (!shouldRetry(ex) || attempt == maxAttempts) {
          log.warn("Naver news collect failed. keyword={}, status={}, attempt={}, message={}",
              keyword, ex.getStatusCode().value(), attempt, ex.getMessage());
          return null;
        }

        log.warn("Retryable Naver response. keyword={}, status={}, attempt={}, message={}",
            keyword, ex.getStatusCode().value(), attempt, ex.getMessage());
      } catch (ResourceAccessException ex) {
        if (attempt == maxAttempts) {
          log.warn("Naver news collect failed. keyword={}, status=NETWORK, attempt={}, message={}",
              keyword, attempt, ex.getMessage());
          return null;
        }

        log.warn("Retryable Naver network error. keyword={}, attempt={}, message={}",
            keyword, attempt, ex.getMessage());
      }

      sleepBeforeRetry(attempt);
    }

    return null;
  }

  /**
   * 실제 Naver 뉴스 검색 API를 호출합니다.
   */
  private NaverNewsResponseDto request(String keyword, int display) {
    return naverRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(NEWS_SEARCH_PATH)
            .queryParam("query", keyword)
            .queryParam("display", display)
            .queryParam("start", 1)
            .queryParam("sort", "date")
            .build())
        .retrieve()
        .body(NaverNewsResponseDto.class);
  }

  /**
   * 429 또는 5xx 응답처럼 다시 시도할 만한 오류인지 판단합니다.
   */
  private boolean shouldRetry(RestClientResponseException ex) {
    int status = ex.getStatusCode().value();
    return status == 429 || status >= 500;
  }

  /**
   * 재시도 전 대기 시간입니다.
   * exponential backoff(재시도할수록 기다리는 시간 점점늘리는 방식)와
   * jitter를 적용해 재시도 요청이 한 시점에 몰리지 않게 합니다.
   */
  private void sleepBeforeRetry(int failedAttempt) {
    // 재시도할수록 기다리는시간 점점 늘림
    long baseDelay = properties.retryInitialDelayMillis() * (1L << Math.max(0, failedAttempt - 1));
    // 최대 대기 시간 넘지 않게 자름
    long cappedDelay = Math.min(baseDelay, properties.retryMaxDelayMillis());
    // 대기 시간에 약간의 랜덤 값 더함(여러 서버나 배치가 동시에 몰리는 것 방지)
    long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1L, cappedDelay / 5L + 1L));

    try {
      Thread.sleep(cappedDelay + jitter);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
