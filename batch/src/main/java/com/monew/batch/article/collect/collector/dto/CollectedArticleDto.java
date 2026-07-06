package com.monew.batch.article.collect.collector.dto;

import com.monew.batch.article.collect.dto.NaverNewsItemResponseDto;
import com.monew.batch.article.entity.ArticleSource;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.springframework.web.util.HtmlUtils;


// 외부 뉴스 api에서 가져온 기사 우리 서비스가 저장하기 좋은 공통 형태로 바꾼 DTO
public record CollectedArticleDto(
    ArticleSource source,     // 뉴스 출처
    String sourceUrl,     // 뉴스 원본 링크
    String title,     // 뉴스 제목
    LocalDateTime publishDate,    // 뉴스 발행 날짜
    String summary      // 뉴스 내용 요약
) {

  // 발행일 저장 기준 시간대. JVM/DB 어디서 돌아도 소스(네이버 +0900, RSS GMT 등)와 무관하게
  // 항상 KST 벽시계로 통일해서 저장해야 정렬/날짜필터가 일관됨
  private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Seoul");

  public static CollectedArticleDto from(NaverNewsItemResponseDto item){
    return new CollectedArticleDto(
        ArticleSource.NAVER,
        item.originallink() != null ? item.originallink(): item.link(),
        cleanHtmlText(item.title()),
        parsePubDate(item.pubDate()),
        cleanHtmlText(item.description())
    );
  }

  public static CollectedArticleDto from(SyndEntry entry, ArticleSource source){
    String sourceUrl = cleanHtmlText(entry.getLink());
    String title = cleanHtmlText(entry.getTitle());
    if (sourceUrl.isBlank() || title.isBlank()) {
      return null;
    }

    return new CollectedArticleDto(
        source,
        sourceUrl,
        title,
        toLocalDateTime(entry),
        cleanHtmlText(descriptionOf(entry))
    );
  }

  private static String cleanHtmlText(String value) {
    if (value == null) {
      return "";
    }
    return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).trim();
  }

  /**
   * Naver pubDate 문자열을 DB 저장용 LocalDateTime으로 변환합니다.
   */
  private static LocalDateTime parsePubDate(String pubDate) {
    if (pubDate == null || pubDate.isBlank()) {
      return LocalDateTime.now(STORAGE_ZONE);
    }
    return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
        .withZoneSameInstant(STORAGE_ZONE)
        .toLocalDateTime();
  }

  /**
   * Rome이 제공하는 publishedDate/updatedDate를 LocalDateTime으로 변환합니다.
   * 날짜가 없으면 배치 전체를 멈추지 않고 현재 시각을 사용합니다.
   */
  private static LocalDateTime toLocalDateTime(SyndEntry entry) {
    Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
    if (date == null) {
      return LocalDateTime.now(STORAGE_ZONE);
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), STORAGE_ZONE);
  }

  /**
   * RSS description 값을 꺼냅니다.
   * description이 없으면 빈 문자열을 반환해 summary가 null이 되지 않게 합니다.
   */
  private static String descriptionOf(SyndEntry entry) {
    SyndContent description = entry.getDescription();
    return description == null ? "" : description.getValue();
  }

}
