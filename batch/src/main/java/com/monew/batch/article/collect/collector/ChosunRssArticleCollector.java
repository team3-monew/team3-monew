package com.monew.batch.article.collect.collector;

import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.config.RssProperties;
import com.monew.batch.article.entity.ArticleSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 조선일보 전체기사 RSS를 수집하는 구현체입니다.
 * RSS URL은 monew.rss.chosun.url 설정값을 사용합니다.
 */
@Component
public class ChosunRssArticleCollector extends AbstractRssArticleCollector {

  private final RssProperties rssProperties;

  public ChosunRssArticleCollector(RestClient.Builder restClientBuilder,
      ArticleCollectProperties properties, RssProperties rssProperties) {
    super(restClientBuilder, properties);
    this.rssProperties = rssProperties;
  }

  @Override
  public ArticleSource getSource() {
    return ArticleSource.CHOSUN;
  }

  @Override
  protected String getFeedUrl() {
    return rssProperties.chosun().url();
  }
}
