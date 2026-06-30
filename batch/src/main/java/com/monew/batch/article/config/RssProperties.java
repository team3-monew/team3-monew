package com.monew.batch.article.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RSS 주소 설정을 application.yml에서 읽어오는 properties 클래스입니다.
 * 한경/조선은 단일 URL, 연합뉴스TV는 카테고리별 여러 URL 목록을 사용합니다.
 */
@ConfigurationProperties(prefix = "monew.rss")
public record RssProperties(
    Feed hankyung,
    Feed chosun,
    FeedList yeonhap
) {
  public record Feed(String url) { }
  public record FeedList(List<String> urls) { }
}
