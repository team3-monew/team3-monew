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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class YeonhapRssArticleCollectorTest {

  @Test
  void collectsCategoryFeedsInOrderAndDeduplicatesBySourceUrl() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    String firstUrl = "http://example.com/politics/feed/";
    String secondUrl = "http://example.com/economy/feed/";

    server.expect(once(), requestTo(firstUrl))
        .andRespond(withSuccess(rss(
            item("A", "http://news.example.com/a"),
            item("B", "http://news.example.com/b")
        ), MediaType.APPLICATION_XML));
    server.expect(once(), requestTo(secondUrl))
        .andRespond(withSuccess(rss(
            item("A duplicated", "http://news.example.com/a"),
            item("C", "http://news.example.com/c")
        ), MediaType.APPLICATION_XML));

    YeonhapRssArticleCollector collector = collector(builder, firstUrl, secondUrl);

    RssCollectResultDto result = collector.collectLatestResult(100);

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
  void continuesNextCategoryWhenOneCategoryFails() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    String firstUrl = "http://example.com/politics/feed/";
    String secondUrl = "http://example.com/economy/feed/";

    server.expect(once(), requestTo(firstUrl)).andRespond(withServerError());
    server.expect(once(), requestTo(secondUrl))
        .andRespond(withSuccess(rss(item("B", "http://news.example.com/b")),
            MediaType.APPLICATION_XML));

    YeonhapRssArticleCollector collector = collector(builder, firstUrl, secondUrl);

    RssCollectResultDto result = collector.collectLatestResult(100);

    assertThat(result.requestCount()).isEqualTo(2);
    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isEqualTo(1);
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("http://news.example.com/b");
    server.verify();
  }

  private YeonhapRssArticleCollector collector(RestClient.Builder builder, String... urls) {
    return new YeonhapRssArticleCollector(builder,
        new ArticleCollectProperties(100, 100, 1, 1, 1, "0 0 * * * *"),
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
