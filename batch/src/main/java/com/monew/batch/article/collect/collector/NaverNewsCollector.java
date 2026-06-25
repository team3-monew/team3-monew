package com.monew.batch.article.collect.collector;

import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.collect.dto.NaverNewsItemResponse;
import com.monew.batch.article.collect.dto.NaverNewsResponse;
import com.monew.batch.article.entity.ArticleSource;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Naver 뉴스 검색 Open API를 호출해 키워드별 최신 뉴스를 수집하는 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsCollector implements KeywordBasedArticleCollector {

  private static final String NEWS_SEARCH_PATH = "/v1/search/news.json";

  private final RestClient naverRestClient;
  private final ArticleCollectProperties properties;

  @Override
  public ArticleSource getSource() {
    return ArticleSource.NAVER;
  }

  // 키워드 1개에 대한 Naver API 호출 + 응답 items 기사 목록으로 변환
  @Override
  public List<CollectedArticle> collectByKeyword(String keyword, int limit) {
    int display = Math.min(limit, properties.naverDisplay());
    NaverNewsResponse response = requestWithRetry(keyword, display);

    if (response == null || response.items() == null) {
      return List.of();
    }

    return response.items().stream()
        .map(this::toCollectedArticle)
        .filter(article -> article.sourceUrl() != null && !article.sourceUrl().isBlank())
        .toList();
  }

  // Naver API 호출 + 재시도
  private NaverNewsResponse requestWithRetry(String keyword, int display) {
    int maxAttempts = Math.max(1, properties.retryMaxAttempts());

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
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
   * 이번 1차 구현에서는 키워드당 start=1, sort=date, display 최대 100으로 한 페이지만 조회합니다.
   */
  private NaverNewsResponse request(String keyword, int display) {
    return naverRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(NEWS_SEARCH_PATH)
            .queryParam("query", keyword)
            .queryParam("display", display)
            .queryParam("start", 1)
            .queryParam("sort", "date")
            .build())
        .retrieve()
        .body(NaverNewsResponse.class);
  }

  private boolean shouldRetry(RestClientResponseException ex) {
    int status = ex.getStatusCode().value();
    return status == 429 || status >= 500;
  }

  private void sleepBeforeRetry(int failedAttempt) {
    long baseDelay = properties.retryInitialDelayMillis() * (1L << Math.max(0, failedAttempt - 1));
    long cappedDelay = Math.min(baseDelay, properties.retryMaxDelayMillis());
    long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1L, cappedDelay / 5L + 1L));

    try {
      Thread.sleep(cappedDelay + jitter);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Naver 응답 item 1건을 우리 배치 공통 DTO인 CollectedArticle로 변환합니다.
   */
  private CollectedArticle toCollectedArticle(NaverNewsItemResponse item) {
    String sourceUrl = firstNotBlank(item.originallink(), item.link());
    return new CollectedArticle(
        ArticleSource.NAVER,
        sourceUrl,
        cleanHtmlText(item.title()),
        parsePubDate(item.pubDate()),
        cleanHtmlText(item.description())
    );
  }

  /**
   * Naver 응답은 originallink가 비어 있을 수 있어, 없으면 link를 원문 URL 후보로 사용합니다.
   */
  private String firstNotBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second;
  }

  /**
   * Naver 응답의 title/description에 포함된 HTML 태그와 entity를 제거합니다.
   */
  private String cleanHtmlText(String value) {
    if (value == null) {
      return "";
    }

    // Naver search returns short snippets with simple tags/entities, so a lightweight cleanup is enough here.
    return Stream.of(value)
        .map(text -> text.replaceAll("<[^>]*>", ""))
        .map(org.springframework.web.util.HtmlUtils::htmlUnescape)
        .map(String::trim)
        .findFirst()
        .orElse("");
  }

  /**
   * Naver pubDate 문자열(RFC 1123 형식)을 DB 저장용 LocalDateTime으로 변환합니다.
   */
  private LocalDateTime parsePubDate(String pubDate) {
    if (pubDate == null || pubDate.isBlank()) {
      return LocalDateTime.now();
    }
    return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
  }
}
