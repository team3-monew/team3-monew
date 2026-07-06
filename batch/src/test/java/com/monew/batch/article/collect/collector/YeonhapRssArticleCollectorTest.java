package com.monew.batch.article.collect.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.config.RssProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YeonhapRssArticleCollectorTest {

  private static final String POLITICS_URL = "http://example.com/politics/feed/";
  private static final String ECONOMY_URL = "http://example.com/economy/feed/";
  private static final String SOCIETY_URL = "http://example.com/society/feed/";

  @Test
  @DisplayName("성공 - 여러 카테고리 feed를 순서대로 요청한다")
  void collectLatestResult_success_requestsCategoryFeedsInOrder() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(POLITICS_URL))
        .andRespond(withSuccess(rss(item("Politics", "http://news.example.com/politics")),
            MediaType.APPLICATION_XML));
    server.expect(once(), requestTo(ECONOMY_URL))
        .andRespond(withSuccess(rss(item("Economy", "http://news.example.com/economy")),
            MediaType.APPLICATION_XML));
    server.expect(once(), requestTo(SOCIETY_URL))
        .andRespond(withSuccess(rss(item("Society", "http://news.example.com/society")),
            MediaType.APPLICATION_XML));
    YeonhapRssArticleCollector collector = collector(builder, POLITICS_URL, ECONOMY_URL,
        SOCIETY_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(100);

    // then
    assertThat(result.requestCount()).isEqualTo(3);
    assertThat(result.successCount()).isEqualTo(3);
    assertThat(result.failureCount()).isZero();
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly(
            "http://news.example.com/politics",
            "http://news.example.com/economy",
            "http://news.example.com/society"
        );
    server.verify();
  }

  @Test
  @DisplayName("성공 - 카테고리 간 같은 sourceUrl은 한 번만 남긴다")
  void collectLatestResult_success_deduplicatesSameSourceUrlAcrossCategories() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(POLITICS_URL))
        .andRespond(withSuccess(rss(
            item("A", "http://news.example.com/a"),
            item("B", "http://news.example.com/b")
        ), MediaType.APPLICATION_XML));
    server.expect(once(), requestTo(ECONOMY_URL))
        .andRespond(withSuccess(rss(
            item("A duplicated", "http://news.example.com/a"),
            item("C", "http://news.example.com/c")
        ), MediaType.APPLICATION_XML));
    YeonhapRssArticleCollector collector = collector(builder, POLITICS_URL, ECONOMY_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(100);

    // then
    assertThat(result.requestCount()).isEqualTo(2);
    assertThat(result.successCount()).isEqualTo(2);
    assertThat(result.failureCount()).isZero();
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly(
            "http://news.example.com/a",
            "http://news.example.com/b",
            "http://news.example.com/c"
        );
    server.verify();
  }

  @Test
  @DisplayName("성공 - 한 카테고리가 실패해도 다음 카테고리를 계속 수집한다")
  void collectLatestResult_success_continuesNextCategory_whenOneCategoryFails() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(POLITICS_URL)).andRespond(withServerError());
    server.expect(once(), requestTo(ECONOMY_URL))
        .andRespond(withSuccess(rss(item("Economy", "http://news.example.com/economy")),
            MediaType.APPLICATION_XML));
    YeonhapRssArticleCollector collector = collector(builder, POLITICS_URL, ECONOMY_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(100);

    // then
    assertThat(result.requestCount()).isEqualTo(2);
    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isEqualTo(1);
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("http://news.example.com/economy");
    server.verify();
  }

  @Test
  @DisplayName("성공 - URL 목록이 비어 있으면 empty 결과를 반환한다")
  void collectLatestResult_success_returnsEmptyResult_whenUrlListIsEmpty() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    YeonhapRssArticleCollector collector = collector(builder);

    // when
    RssCollectResultDto result = collector.collectLatestResult(100);

    // then
    assertThat(result).isEqualTo(RssCollectResultDto.empty());
    server.verify();
  }

  @Test
  @DisplayName("성공 - 전체 request/success/failure count가 카테고리별 결과 합산과 일치한다")
  void collectLatestResult_success_sumsRequestSuccessFailureCountsByCategory() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(POLITICS_URL))
        .andRespond(withSuccess(rss(item("Politics", "http://news.example.com/politics")),
            MediaType.APPLICATION_XML));
    server.expect(once(), requestTo(ECONOMY_URL)).andRespond(withServerError());
    server.expect(once(), requestTo(SOCIETY_URL))
        .andRespond(withSuccess("<rss><channel><item>", MediaType.APPLICATION_XML));
    YeonhapRssArticleCollector collector = collector(builder, POLITICS_URL, ECONOMY_URL,
        SOCIETY_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(100);

    // then
    assertThat(result.requestCount()).isEqualTo(3);
    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isEqualTo(2);
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("http://news.example.com/politics");
    server.verify();
  }

  @Test
  @DisplayName("성공 - limit이 각 feed 요청에 적용된다")
  void collectLatestResult_success_appliesLimitToEachFeedRequest() {
    // given
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(once(), requestTo(POLITICS_URL))
        .andRespond(withSuccess(rss(
            item("Politics A", "http://news.example.com/politics-a"),
            item("Politics B", "http://news.example.com/politics-b")
        ), MediaType.APPLICATION_XML));
    server.expect(once(), requestTo(ECONOMY_URL))
        .andRespond(withSuccess(rss(
            item("Economy A", "http://news.example.com/economy-a"),
            item("Economy B", "http://news.example.com/economy-b")
        ), MediaType.APPLICATION_XML));
    YeonhapRssArticleCollector collector = collector(builder, POLITICS_URL, ECONOMY_URL);

    // when
    RssCollectResultDto result = collector.collectLatestResult(1);

    // then
    assertThat(result.requestCount()).isEqualTo(2);
    assertThat(result.successCount()).isEqualTo(2);
    assertThat(result.failureCount()).isZero();
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly(
            "http://news.example.com/politics-a",
            "http://news.example.com/economy-a"
        );
    server.verify();
  }

  private YeonhapRssArticleCollector collector(RestClient.Builder builder, String... urls) {
    return new YeonhapRssArticleCollector(builder,
        new ArticleCollectProperties(100, 100, 1, 0, 0, "0 0 * * * *"),
        new RssProperties(
            new RssProperties.Feed("http://example.com/hankyung/feed/"),
            new RssProperties.Feed("http://example.com/chosun/feed/"),
            new RssProperties.FeedList(List.of(urls))
        ));
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
