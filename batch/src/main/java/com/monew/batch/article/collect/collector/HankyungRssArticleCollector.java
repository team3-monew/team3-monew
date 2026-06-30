package com.monew.batch.article.collect.collector;

import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.config.RssProperties;
import com.monew.batch.article.entity.ArticleSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 한국경제 RSS를 수집하는 구현체입니다.
 * RSS URL은 monew.rss.hankyung.url 설정값을 사용합니다.
 */
@Component
public class HankyungRssArticleCollector extends AbstractRssArticleCollector {

  private final RssProperties rssProperties;

  public HankyungRssArticleCollector(RestClient.Builder restClientBuilder,
      ArticleCollectProperties properties, RssProperties rssProperties) {
    super(restClientBuilder, properties);
    this.rssProperties = rssProperties;
  }

  @Override
  public ArticleSource getSource() {
    return ArticleSource.HANKYUNG;
  }

  @Override
  protected String getFeedUrl() {
    return rssProperties.hankyung().url();
  }
}
