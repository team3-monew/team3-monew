package com.monew.batch.article.collect.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.KeywordCollectResultDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.config.NaverApiProperties;
import com.monew.batch.article.entity.ArticleSource;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NaverNewsCollectorTest {

  private static final String BASE_URL = "https://openapi.naver.com";
  private static final String NEWS_SEARCH_URL = BASE_URL + "/v1/search/news.json";
  private static final String PUB_DATE = "Fri, 03 Jul 2026 10:00:00 +0900";

  @Test
  @DisplayName("성공 - Naver 응답 items를 CollectedArticleDto로 변환한다")
  void collectByKeywordResult_success_convertItemsToCollectedArticleDto() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build());

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withSuccess(responseJson(itemJson(
            "Java News",
            "https://original.example.com/java",
            "https://naver.example.com/java",
            "Java summary"
        )), MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isZero();
    assertThat(result.articles()).hasSize(1);
    assertThat(result.articles().get(0))
        .extracting(
            CollectedArticleDto::source,
            CollectedArticleDto::sourceUrl,
            CollectedArticleDto::title,
            CollectedArticleDto::summary
        )
        .containsExactly(
            ArticleSource.NAVER,
            "https://original.example.com/java",
            "Java News",
            "Java summary"
        );
    assertThat(result.articles().get(0).publishDate())
        .isEqualTo(LocalDateTime.of(2026, 7, 3, 10, 0));
    server.verify();
  }

  @Test
  @DisplayName("성공 - originallink가 있으면 link보다 originallink를 sourceUrl로 사용한다")
  void collectByKeywordResult_success_useOriginalLinkFirst() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build());

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withSuccess(responseJson(itemJson(
            "Java News",
            "https://original.example.com/java",
            "https://naver.example.com/java",
            "Java summary"
        )), MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("https://original.example.com/java");
    server.verify();
  }

  @Test
  @DisplayName("성공 - originallink가 없으면 link를 sourceUrl로 사용한다")
  void collectByKeywordResult_success_useLinkWhenOriginalLinkIsNull() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build());

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withSuccess(responseJson(itemJson(
            "Java News",
            null,
            "https://naver.example.com/java",
            "Java summary"
        )), MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("https://naver.example.com/java");
    server.verify();
  }

  @Test
  @DisplayName("성공 - title과 description의 HTML 태그와 escape 문자를 제거한다")
  void collectByKeywordResult_success_cleanHtmlTagsAndEscapedText() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build());

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withSuccess(responseJson(itemJson(
            "<b>Java</b> &amp; Spring",
            "https://original.example.com/java",
            "https://naver.example.com/java",
            "<p>Spring &lt;Boot&gt; summary</p>"
        )), MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result.articles().get(0).title()).isEqualTo("Java & Spring");
    assertThat(result.articles().get(0).summary()).isEqualTo("Spring <Boot> summary");
    server.verify();
  }

  @Test
  @DisplayName("성공 - items가 null이면 빈 기사 목록과 successCount 1을 반환한다")
  void collectByKeywordResult_success_returnEmptyArticlesWhenItemsIsNull() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build());

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withSuccess("{\"items\":null}", MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result.articles()).isEmpty();
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isEqualTo(1);
    assertThat(result.failureCount()).isZero();
    server.verify();
  }

  @Test
  @DisplayName("성공 - sourceUrl이 null이거나 blank인 기사는 필터링한다")
  void collectByKeywordResult_success_filterArticlesWithNullOrBlankSourceUrl() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build());

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withSuccess(responseJson(
            itemJson("No URL", null, null, "summary"),
            itemJson("Blank URL", " ", "https://naver.example.com/blank", "summary"),
            itemJson("Valid URL", null, "https://naver.example.com/java", "summary")
        ), MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result.articles())
        .extracting(CollectedArticleDto::sourceUrl)
        .containsExactly("https://naver.example.com/java");
    server.verify();
  }

  @Test
  @DisplayName("성공 - 요청 limit과 properties.naverDisplay 중 작은 값을 display로 사용한다")
  void collectByKeywordResult_success_useSmallerDisplayBetweenLimitAndProperty() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build(), properties(50, 1));

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 30))))
        .andRespond(withSuccess(responseJson(), MediaType.APPLICATION_JSON));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 30);

    // then
    assertThat(result.successCount()).isEqualTo(1);
    server.verify();
  }

  @Test
  @DisplayName("실패 - Naver API 인증 정보가 비어 있으면 요청하지 않고 empty 결과를 반환한다")
  void collectByKeywordResult_fail_returnEmptyResultWithoutRequest_whenCredentialIsMissing() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = new NaverNewsCollector(
        builder.build(),
        properties(100, 1),
        new NaverApiProperties(BASE_URL, "", "client-secret")
    );

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertThat(result).isEqualTo(new KeywordCollectResultDto(List.of(), 0, 0, 0));
    server.verify();
  }

  @Test
  @DisplayName("실패 - 500 또는 429 응답이면 재시도 후 최종 실패 결과를 반환한다")
  void collectByKeywordResult_fail_retryOnServerErrorOrTooManyRequests() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build(), properties(100, 2));

    server.expect(times(2), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertFailureResult(result);
    server.verify();
  }

  @Test
  @DisplayName("실패 - 400 같은 재시도 불필요 응답이면 재시도하지 않고 실패 결과를 반환한다")
  void collectByKeywordResult_fail_doNotRetryOnBadRequest() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build(), properties(100, 2));

    server.expect(once(), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withBadRequest());

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertFailureResult(result);
    server.verify();
  }

  @Test
  @DisplayName("실패 - 네트워크 예외가 발생하면 재시도 후 실패 결과를 반환한다")
  void collectByKeywordResult_fail_retryOnNetworkException() {
    // given
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    NaverNewsCollector collector = collector(builder.build(), properties(100, 2));

    server.expect(times(2), requestTo(URI.create(NEWS_SEARCH_URL + requestQuery("java", 10))))
        .andRespond(withException(new SocketTimeoutException("network error")));

    // when
    KeywordCollectResultDto result = collector.collectByKeywordResult("java", 10);

    // then
    assertFailureResult(result);
    server.verify();
  }

  private NaverNewsCollector collector(RestClient restClient) {
    return collector(restClient, properties(100, 1));
  }

  private NaverNewsCollector collector(RestClient restClient, ArticleCollectProperties properties) {
    return new NaverNewsCollector(restClient, properties,
        new NaverApiProperties(BASE_URL, "client-id", "client-secret"));
  }

  private ArticleCollectProperties properties(int naverDisplay, int retryMaxAttempts) {
    return new ArticleCollectProperties(
        naverDisplay,
        100,
        retryMaxAttempts,
        0,
        0,
        "0 0 * * * *"
    );
  }

  private void assertFailureResult(KeywordCollectResultDto result) {
    assertThat(result.articles()).isEmpty();
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
  }

  private String requestQuery(String keyword, int display) {
    return "?query=%s&display=%d&start=1&sort=date".formatted(keyword, display);
  }

  private String responseJson(String... items) {
    return """
        {
          "items": [%s]
        }
        """.formatted(String.join(",", items));
  }

  private String itemJson(String title, String originalLink, String link, String description) {
    return """
        {
          "title": %s,
          "originallink": %s,
          "link": %s,
          "description": %s,
          "pubDate": "%s"
        }
        """.formatted(
        jsonValue(title),
        jsonValue(originalLink),
        jsonValue(link),
        jsonValue(description),
        PUB_DATE
    );
  }

  private String jsonValue(String value) {
    if (value == null) {
      return "null";
    }
    return "\"" + value.replace("\"", "\\\"") + "\"";
  }
}
