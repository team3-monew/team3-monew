package com.monew.batch.article.collect.collector;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * RSS 수집기들이 공유하는 공통 로직을 담은 추상 클래스입니다.
 * RSS XML 요청, 일시적 오류 재시도, Rome 기반 RSS 파싱, CollectedArticleDto 변환을 담당합니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRssArticleCollector implements FeedBasedArticleCollector {

  private final RestClient.Builder restClientBuilder;
  private final ArticleCollectProperties properties;

  /**
   * 단일 RSS URL을 사용하는 수집기의 기본 수집 흐름입니다.
   */
  @Override
  public RssCollectResultDto collectLatestResult(int limit) {
    return collectFeed(getFeedUrl(), limit);
  }

  /**
   * 하위 클래스가 자신이 호출할 RSS URL을 제공하도록 합니다.
   */
  protected abstract String getFeedUrl();

  /**
   * RSS URL 하나를 수집합니다.
   * 연합뉴스TV처럼 여러 URL을 도는 수집기는 이 메서드를 URL 개수만큼 반복 호출합니다.
   */
  protected RssCollectResultDto collectFeed(String feedUrl, int limit) {
    if (feedUrl == null || feedUrl.isBlank()) {
      log.info("Skip RSS collect because feed URL is empty. source={}", getSource());
      return RssCollectResultDto.empty();
    }

    String xml = requestWithRetry(feedUrl);
    if (xml == null || xml.isBlank()) {
      return new RssCollectResultDto(List.of(), 1, 0, 1);
    }

    try {
      List<CollectedArticleDto> articles = parseItems(xml, Math.min(limit, properties.rssLimit()));
      return new RssCollectResultDto(articles, 1, 1, 0);
    } catch (Exception ex) {
      log.warn("RSS parse failed. source={}, url={}, message={}", getSource(), feedUrl,
          ex.getMessage());
      return new RssCollectResultDto(List.of(), 1, 0, 1);
    }
  }

  /**
   * RSS XML을 HTTP로 가져오고, 네트워크 오류나 5xx 응답이면 설정된 횟수만큼 재시도합니다.
   */
  private String requestWithRetry(String feedUrl) {
    int maxAttempts = Math.max(1, properties.retryMaxAttempts());

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        String xml = restClientBuilder.build()
            .get()
            .uri(feedUrl)
            .header(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
            .header(HttpHeaders.ACCEPT,
                String.join(", ",
                    MediaType.APPLICATION_RSS_XML_VALUE,
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.TEXT_XML_VALUE,
                    MediaType.TEXT_HTML_VALUE,
                    MediaType.ALL_VALUE))
            .retrieve()
            .body(String.class);

        log.info("RSS collect request succeeded. source={}, url={}, attempt={}", getSource(),
            feedUrl, attempt);
        return xml;
      } catch (RestClientResponseException ex) {
        if (!shouldRetry(ex) || attempt == maxAttempts) {
          log.warn("RSS collect failed. source={}, url={}, status={}, attempt={}, message={}",
              getSource(), feedUrl, ex.getStatusCode().value(), attempt, ex.getMessage());
          return null;
        }

        log.warn("Retryable RSS response. source={}, url={}, status={}, attempt={}, message={}",
            getSource(), feedUrl, ex.getStatusCode().value(), attempt, ex.getMessage());
      } catch (ResourceAccessException ex) {
        if (attempt == maxAttempts) {
          log.warn("RSS collect failed. source={}, url={}, status=NETWORK, attempt={}, message={}",
              getSource(), feedUrl, attempt, ex.getMessage());
          return null;
        }

        log.warn("Retryable RSS network error. source={}, url={}, attempt={}, message={}",
            getSource(), feedUrl, attempt, ex.getMessage());
      }

      sleepBeforeRetry(attempt);
    }

    return null;
  }

  /**
   * RSS 호출에서 재시도할 HTTP 상태인지 판단합니다.
   * 400/401/403/404는 요청 자체 문제로 보고 재시도하지 않습니다.
   */
  private boolean shouldRetry(RestClientResponseException ex) {
    int status = ex.getStatusCode().value();
    return status >= 500;
  }

  /**
   * 재시도 전 대기 시간입니다.
   * exponential backoff에 약간의 jitter를 더해 동시에 재시도 요청이 몰리는 것을 줄입니다.
   */
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
   * Rome 라이브러리로 RSS XML을 파싱해 CollectedArticleDto 목록으로 변환합니다.
   */
  protected List<CollectedArticleDto> parseItems(String xml, int limit) throws Exception {
    SyndFeed feed = new SyndFeedInput().build(new StringReader(xml));

    return feed.getEntries().stream()
        .map(entry -> CollectedArticleDto.from(entry, getSource()))
        .filter(Objects::nonNull)
        .limit(limit)
        .toList();
  }

}
