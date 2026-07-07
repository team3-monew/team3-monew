package com.monew.batch.article.collect.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.config.RssProperties;
import com.monew.batch.article.entity.ArticleSource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HankyungRssArticleCollectorTest {

  private static final String FEED_URL = "http://example.com/hankyung/feed/";

  @Test
  @DisplayName("성공 - RSS XML item을 CollectedArticleDto로 변환한다")
  void collectLatestResult_success_convertsRssItemToCollectedArticleDto() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess(rss(item("Economic News", "http://news.example.com/a")),
            MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isZero();
    assertThat(result.articles()).hasSize(1);

    CollectedArticleDto article = result.articles().get(0);
    assertThat(article.source()).isEqualTo(ArticleSource.HANKYUNG);
    assertThat(article.sourceUrl()).isEqualTo("http://news.example.com/a");
    assertThat(article.title()).isEqualTo("Economic News");
    assertThat(article.summary()).isEqualTo("summary");
    server.verify();
  }

  @Test
  @DisplayName("성공 - limit만큼만 수집한다")
  void collectLatestResult_success_collectsOnlyLimit() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess(rss(
            item("First", "http://news.example.com/a"),
            item("Second", "http://news.example.com/b"),
            item("Third", "http://news.example.com/c")
        ), MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(2);

    // then
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("http://news.example.com/a", "http://news.example.com/b");
    server.verify();
  }

  @Test
  @DisplayName("성공 - RSS item의 title/link/description/pubDate를 올바르게 매핑한다")
  void collectLatestResult_success_mapsRssItemFields() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess(rss("""
            <item>
              <title><![CDATA[<b>Market Rises</b>]]></title>
              <link>http://news.example.com/market</link>
              <pubDate>Thu, 26 Jun 2025 10:00:00 +0900</pubDate>
              <description><![CDATA[<p>KOSPI closes higher</p>]]></description>
            </item>
            """), MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    CollectedArticleDto article = result.articles().get(0);
    assertThat(article.title()).isEqualTo("Market Rises");
    assertThat(article.sourceUrl()).isEqualTo("http://news.example.com/market");
    assertThat(article.summary()).isEqualTo("KOSPI closes higher");
    assertThat(article.publishDate())
        .isEqualTo(localDateTimeFromRfc1123("Thu, 26 Jun 2025 10:00:00 +0900"));
    server.verify();
  }

  @Test
  @DisplayName("성공 - description이 없으면 summary를 빈 문자열로 처리한다")
  void collectLatestResult_success_usesEmptySummary_whenDescriptionIsMissing() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess(rss("""
            <item>
              <title>No description article</title>
              <link>http://news.example.com/no-description</link>
              <pubDate>Thu, 26 Jun 2025 10:00:00 +0900</pubDate>
            </item>
            """), MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertThat(result.articles()).hasSize(1);
    assertThat(result.articles().get(0).summary()).isEmpty();
    server.verify();
  }

  @Test
  @DisplayName("성공 - publishedDate가 없고 updatedDate가 있으면 updatedDate를 사용한다")
  void collectLatestResult_success_usesUpdatedDate_whenPublishedDateIsMissing() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess(atom("""
            <entry>
              <title>Updated article</title>
              <link href="http://news.example.com/updated" />
              <updated>2025-06-26T11:30:00+09:00</updated>
              <summary>Updated summary</summary>
            </entry>
            """), MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertThat(result.articles()).hasSize(1);
    assertThat(result.articles().get(0).publishDate())
        .isEqualTo(localDateTimeFromOffset("2025-06-26T11:30:00+09:00"));
    server.verify();
  }

  @Test
  @DisplayName("성공 - title 또는 link가 비어 있으면 해당 item을 제외한다")
  void collectLatestResult_success_excludesItem_whenTitleOrLinkIsBlank() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess(rss(
            item("", "http://news.example.com/blank-title"),
            item("Blank link", ""),
            item("Valid article", "http://news.example.com/valid")
        ), MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("http://news.example.com/valid");
    server.verify();
  }

  @Test
  @DisplayName("실패 - feed URL이 비어 있으면 요청하지 않고 empty 결과를 반환한다")
  void collectLatestResult_failure_returnsEmptyResultWithoutRequest_whenFeedUrlIsBlank() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(never(), requestTo(FEED_URL));
    HankyungRssArticleCollector collector = collector(builder, " ");

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertThat(result).isEqualTo(RssCollectResultDto.empty());
    server.verify();
  }

  @Test
  @DisplayName("실패 - RSS 파싱이 실패하면 failureCount=1을 반환한다")
  void collectLatestResult_failure_returnsFailureCountOne_whenRssParsingFails() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL))
        .andRespond(withSuccess("<rss><channel><item>", MediaType.APPLICATION_XML));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertThat(result.articles()).isEmpty();
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
    server.verify();
  }

  @Test
  @DisplayName("실패 - 500/429 응답이면 재시도 후 실패 결과를 반환한다")
  void collectLatestResult_failure_retriesAndReturnsFailureResult_whenResponseIs500Or429() {
    // given
    RestClient.Builder serverErrorBuilder = RestClient.builder();
    MockRestServiceServer serverErrorServer = MockRestServiceServer.bindTo(serverErrorBuilder).build();
    serverErrorServer.expect(times(2), requestTo(FEED_URL)).andRespond(withServerError());
    HankyungRssArticleCollector serverErrorCollector = collector(serverErrorBuilder, FEED_URL, 2);

    RestClient.Builder tooManyRequestsBuilder = RestClient.builder();
    MockRestServiceServer tooManyRequestsServer = MockRestServiceServer.bindTo(tooManyRequestsBuilder)
        .build();
    tooManyRequestsServer.expect(times(2), requestTo(FEED_URL))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    HankyungRssArticleCollector tooManyRequestsCollector = collector(tooManyRequestsBuilder, FEED_URL,
        2);

    // when
    RssCollectResultDto serverErrorResult = serverErrorCollector.collectLatestResult(10);
    RssCollectResultDto tooManyRequestsResult = tooManyRequestsCollector.collectLatestResult(10);

    // then
    assertFailureResult(serverErrorResult);
    assertFailureResult(tooManyRequestsResult);
    serverErrorServer.verify();
    tooManyRequestsServer.verify();
  }

  @Test
  @DisplayName("실패 - 400 응답이면 재시도하지 않고 실패 결과를 반환한다")
  void collectLatestResult_failure_doesNotRetry_whenResponseIs400() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(FEED_URL)).andRespond(withBadRequest());
    HankyungRssArticleCollector collector = collector(builder, FEED_URL, 3);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertFailureResult(result);
    server.verify();
  }

  @Test
  @DisplayName("실패 - 네트워크 예외가 발생하면 재시도 후 실패 결과를 반환한다")
  void collectLatestResult_failure_retriesAndReturnsFailureResult_whenNetworkExceptionOccurs() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(times(2), requestTo(FEED_URL))
        .andRespond(withException(new IOException("network error")));
    HankyungRssArticleCollector collector = collector(builder, FEED_URL, 2);

    // when
    RssCollectResultDto result = collector.collectLatestResult(10);

    // then
    assertFailureResult(result);
    server.verify();
  }

  private HankyungRssArticleCollector collector(RestClient.Builder builder, String feedUrl) {
    return collector(builder, feedUrl, 1);
  }

  private HankyungRssArticleCollector collector(RestClient.Builder builder, String feedUrl,
      int retryMaxAttempts) {
    return new HankyungRssArticleCollector(builder,
        new ArticleCollectProperties(100, 100, retryMaxAttempts, 0, 0, "0 0 * * * *"),
        new RssProperties(
            new RssProperties.Feed(feedUrl),
            new RssProperties.Feed("http://example.com/chosun/feed/"),
            new RssProperties.FeedList(List.of("http://example.com/yeonhap/feed/"))
        ));
  }

  private void assertFailureResult(RssCollectResultDto result) {
    assertThat(result.articles()).isEmpty();
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
  }

  // 프로덕션 코드가 발행일을 KST(Asia/Seoul) 벽시계로 고정 저장하므로 테스트 기대값도 KST 기준
  // (systemDefault를 쓰면 UTC로 도는 CI 러너에서 기대값이 달라져 깨짐)
  private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

  private LocalDateTime localDateTimeFromRfc1123(String value) {
    return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
        .withZoneSameInstant(STORAGE_ZONE)
        .toLocalDateTime();
  }

  private LocalDateTime localDateTimeFromOffset(String value) {
    return OffsetDateTime.parse(value)
        .atZoneSameInstant(STORAGE_ZONE)
        .toLocalDateTime();
  }

  private String rss(String... items) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            %s
          </channel>
        </rss>
        """.formatted(String.join("\n", items));
  }

  private String atom(String... entries) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Hankyung</title>
          %s
        </feed>
        """.formatted(String.join("\n", entries));
  }

  private String item(String title, String link) {
    return """
        <item>
          <title>%s</title>
          <link>%s</link>
          <pubDate>Thu, 26 Jun 2025 10:00:00 +0900</pubDate>
          <description>summary</description>
        </item>
        """.formatted(title, link);
  }
}
