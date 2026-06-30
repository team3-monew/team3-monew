package com.monew.batch.article.collect.collector;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.config.RssProperties;
import com.monew.batch.article.entity.ArticleSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 연합뉴스TV 카테고리별 RSS 목록을 순서대로 수집하는 구현체입니다.
 * 최신 RSS 단일 URL은 사용하지 않고, monew.rss.yeonhap.urls 목록을 순회합니다.
 */
@Slf4j
@Component
public class YeonhapRssArticleCollector extends AbstractRssArticleCollector {

  private final RssProperties rssProperties;

  public YeonhapRssArticleCollector(RestClient.Builder restClientBuilder,
      ArticleCollectProperties properties, RssProperties rssProperties) {
    super(restClientBuilder, properties);
    this.rssProperties = rssProperties;
  }

  @Override
  public ArticleSource getSource() {
    return ArticleSource.YEONHAP;
  }

  /**
   * 연합뉴스TV는 단일 URL 흐름을 쓰지 않으므로 null을 반환합니다.
   * 실제 수집은 collectLatestResult에서 URL 목록을 직접 순회합니다.
   */
  @Override
  protected String getFeedUrl() {
    return null;
  }

  @Override
  public RssCollectResultDto collectLatestResult(int limit) {
    List<String> urls = rssProperties.yeonhap().urls();
    if (urls == null || urls.isEmpty()) {
      return RssCollectResultDto.empty();
    }

    Map<String, CollectedArticleDto> articlesByUrl = new LinkedHashMap<>();
    int requestCount = 0;
    int successCount = 0;
    int failureCount = 0;
    int collectedBeforeDeduplication = 0;

    for (String url : urls) {
      RssCollectResultDto result = collectFeed(url, limit);
      requestCount += result.requestCount();
      successCount += result.successCount();
      failureCount += result.failureCount();
      collectedBeforeDeduplication += result.articles().size();

      for (CollectedArticleDto article : result.articles()) {
        articlesByUrl.putIfAbsent(article.sourceUrl(), article);
      }
    }

    int duplicateSkippedCount = collectedBeforeDeduplication - articlesByUrl.size();
    log.info("Yonhap RSS collect finished. requestCount={}, successCount={}, failureCount={}, collected={}, duplicateSkipped={}",
        requestCount, successCount, failureCount, articlesByUrl.size(), duplicateSkippedCount);

    return new RssCollectResultDto(new ArrayList<>(articlesByUrl.values()), requestCount, successCount,
        failureCount);
  }
}
