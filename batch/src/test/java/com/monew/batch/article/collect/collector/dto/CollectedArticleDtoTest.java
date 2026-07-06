package com.monew.batch.article.collect.collector.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.batch.article.collect.dto.NaverNewsItemResponseDto;
import com.monew.batch.article.entity.ArticleSource;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectedArticleDtoTest {

  @Test
  @DisplayName("성공 - Naver item의 HTML 태그를 제거한다")
  void from_success_removesHtmlTagsFromNaverItem() {
    // given
    NaverNewsItemResponseDto item = new NaverNewsItemResponseDto(
        "<b>Java</b> &amp; Spring",
        "https://original.example.com/java",
        "https://naver.example.com/java",
        "<p>Spring &lt;Boot&gt; summary</p>",
        "Fri, 03 Jul 2026 10:00:00 +0900"
    );

    // when
    CollectedArticleDto result = CollectedArticleDto.from(item);

    // then
    assertThat(result.title()).isEqualTo("Java & Spring");
    assertThat(result.summary()).isEqualTo("Spring <Boot> summary");
  }

  @Test
  @DisplayName("성공 - RSS entry의 HTML description을 정리한다")
  void from_success_cleansHtmlDescriptionFromRssEntry() {
    // given
    SyndEntry entry = rssEntry(
        "RSS title",
        "https://news.example.com/rss",
        "<p>RSS &lt;summary&gt;</p>",
        dateFromRfc1123("Thu, 26 Jun 2025 10:00:00 +0900")
    );

    // when
    CollectedArticleDto result = CollectedArticleDto.from(entry, ArticleSource.HANKYUNG);

    // then
    assertThat(result.summary()).isEqualTo("RSS <summary>");
  }

  @Test
  @DisplayName("성공 - Naver pubDate를 LocalDateTime으로 변환한다")
  void from_success_convertsNaverPubDateToLocalDateTime() {
    // given
    String pubDate = "Fri, 03 Jul 2026 10:00:00 +0900";
    NaverNewsItemResponseDto item = new NaverNewsItemResponseDto(
        "Java News",
        "https://original.example.com/java",
        "https://naver.example.com/java",
        "Java summary",
        pubDate
    );

    // when
    CollectedArticleDto result = CollectedArticleDto.from(item);

    // then
    assertThat(result.publishDate())
        .isEqualTo(ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toLocalDateTime());
  }

  @Test
  @DisplayName("성공 - RSS publishedDate를 LocalDateTime으로 변환한다")
  void from_success_convertsRssPublishedDateToLocalDateTime() {
    // given
    Date publishedDate = dateFromRfc1123("Thu, 26 Jun 2025 10:00:00 +0900");
    SyndEntry entry = rssEntry(
        "RSS title",
        "https://news.example.com/rss",
        "RSS summary",
        publishedDate
    );

    // when
    CollectedArticleDto result = CollectedArticleDto.from(entry, ArticleSource.HANKYUNG);

    // then
    assertThat(result.publishDate()).isEqualTo(localDateTimeFromDate(publishedDate));
  }

  @Test
  @DisplayName("성공 - RSS title/link가 비어 있으면 null을 반환한다")
  void from_success_returnsNull_whenRssTitleOrLinkIsBlank() {
    // given
    SyndEntry blankTitleEntry = rssEntry(
        " ",
        "https://news.example.com/rss",
        "RSS summary",
        dateFromRfc1123("Thu, 26 Jun 2025 10:00:00 +0900")
    );
    SyndEntry blankLinkEntry = rssEntry(
        "RSS title",
        " ",
        "RSS summary",
        dateFromRfc1123("Thu, 26 Jun 2025 10:00:00 +0900")
    );

    // when
    CollectedArticleDto blankTitleResult = CollectedArticleDto.from(blankTitleEntry,
        ArticleSource.HANKYUNG);
    CollectedArticleDto blankLinkResult = CollectedArticleDto.from(blankLinkEntry,
        ArticleSource.HANKYUNG);

    // then
    assertThat(blankTitleResult).isNull();
    assertThat(blankLinkResult).isNull();
  }

  private SyndEntry rssEntry(String title, String link, String description, Date publishedDate) {
    SyndEntryImpl entry = new SyndEntryImpl();
    entry.setTitle(title);
    entry.setLink(link);
    entry.setPublishedDate(publishedDate);

    SyndContentImpl syndDescription = new SyndContentImpl();
    syndDescription.setValue(description);
    entry.setDescription(syndDescription);

    return entry;
  }

  private Date dateFromRfc1123(String value) {
    Instant instant = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
    return Date.from(instant);
  }

  private LocalDateTime localDateTimeFromDate(Date date) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
  }
}
