package com.monew.server.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.server.article.config.BackupProperties;
import com.monew.server.article.dto.ArticleBackupDto;
import com.monew.server.article.dto.ArticleRestoreResultDto;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.article.storage.ArticleBackupReader;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.article.ArticleErrorCode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleRestoreServiceTest {

  private static final String KEY_2026_07_01 =
      "backups/articles/date=2026-07-01/articles-2026-07-01.json";
  private static final String KEY_2026_07_02 =
      "backups/articles/date=2026-07-02/articles-2026-07-02.json";

  @Mock
  ArticleRepository articleRepository;

  @Mock
  ArticleBackupReader articleBackupReader;

  @Mock
  ObjectMapper objectMapper;

  ArticleRestoreServiceImpl articleRestoreService;

  @BeforeEach
  void setUp() {
    BackupProperties backupProperties = new BackupProperties(
        new BackupProperties.Storage(
            "s3",
            new BackupProperties.Local(Path.of(".monew/backups")),
            new BackupProperties.S3("monew-test", "/backups/articles/", "ap-northeast-2")
        )
    );
    articleRestoreService = new ArticleRestoreServiceImpl(
        articleRepository,
        articleBackupReader,
        backupProperties,
        objectMapper
    );
  }

  @Test
  @DisplayName("실패 - from이 null이면 잘못된 기사 복구 요청 예외가 발생한다")
  void restore_fail_fromIsNull() {
    // given
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);

    // when & then
    assertThatThrownBy(() -> articleRestoreService.restore(null, to))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);

    verifyNoInteractions(articleBackupReader, articleRepository);
  }

  @Test
  @DisplayName("실패 - from이 to보다 같거나 늦으면 잘못된 기사 복구 요청 예외가 발생한다")
  void restore_fail_fromIsNotBeforeTo() {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 2, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);

    // when & then
    assertThatThrownBy(() -> articleRestoreService.restore(from, to))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);

    verifyNoInteractions(articleBackupReader, articleRepository);
  }

  @Test
  @DisplayName("성공 - to가 자정이면 종료일을 제외하고 백업 파일을 조회한다")
  void restore_success_excludesEndDateWhenToIsMidnight() {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(false);
    given(articleRepository.saveAll(any())).willReturn(List.of());

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).restoredArticleCount()).isZero();
    assertThat(result.get(0).restoredArticleIds()).isEmpty();

    then(articleBackupReader).should().exists(KEY_2026_07_01);
    then(articleBackupReader).should(never()).exists(KEY_2026_07_02);
  }

  @Test
  @DisplayName("성공 - to가 자정이 아니면 종료일을 포함해서 백업 파일을 조회한다")
  void restore_success_includesEndDateWhenToIsNotMidnight() {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 10, 0);

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(false);
    given(articleBackupReader.exists(KEY_2026_07_02)).willReturn(false);
    given(articleRepository.saveAll(any())).willReturn(List.of());

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).restoredArticleCount()).isZero();
    assertThat(result.get(0).restoredArticleIds()).isEmpty();

    then(articleBackupReader).should().exists(KEY_2026_07_01);
    then(articleBackupReader).should().exists(KEY_2026_07_02);
  }

  @Test
  @DisplayName("성공 - 백업 파일이 없으면 복구하지 않고 빈 결과를 반환한다")
  void restore_success_backupFileNotExists() {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(false);
    given(articleRepository.saveAll(any())).willReturn(List.of());

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).restoredArticleCount()).isZero();
    assertThat(result.get(0).restoredArticleIds()).isEmpty();

    then(articleBackupReader).should(never()).download(anyString());
    then(articleRepository).should(never()).findExistingSourceUrls(anyCollection());
    then(articleRepository).should().saveAll(List.of());
  }

  @Test
  @DisplayName("성공 - 유효한 백업 기사 2개를 복구하고 복구된 기사 ID를 반환한다")
  void restore_success_restoreValidBackupArticles() throws Exception {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);
    byte[] json = "{}".getBytes();
    ArticleBackupDto first = backupDto("https://news.monew.test/1", "첫 번째 기사", 1, 10);
    ArticleBackupDto second = backupDto("https://news.monew.test/2", "두 번째 기사", 2, 20);

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(true);
    given(articleBackupReader.download(KEY_2026_07_01)).willReturn(json);
    given(objectMapper.readValue(eq(json), anyBackupDtoListType()))
        .willReturn(List.of(first, second));
    given(articleRepository.findExistingSourceUrls(List.of(first.sourceUrl(), second.sourceUrl())))
        .willReturn(Set.of());
    given(articleRepository.saveAll(any())).willAnswer(invocation -> toList(invocation.getArgument(0)));

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    ArgumentCaptor<Iterable<Article>> captor = ArgumentCaptor.forClass(Iterable.class);
    then(articleRepository).should().saveAll(captor.capture());
    List<Article> savedArticles = toList(captor.getValue());

    assertThat(savedArticles).hasSize(2);
    assertThat(savedArticles.get(0).getSourceUrl()).isEqualTo(first.sourceUrl());
    assertThat(savedArticles.get(0).getTitle()).isEqualTo(first.title());
    assertThat(savedArticles.get(0).getPublishDate()).isEqualTo(first.publishDate());
    assertThat(savedArticles.get(0).getCommentCount()).isEqualTo(first.commentCount());
    assertThat(savedArticles.get(0).getViewCount()).isEqualTo(first.viewCount());
    assertThat(savedArticles.get(1).getSourceUrl()).isEqualTo(second.sourceUrl());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).restoredArticleCount()).isEqualTo(2);
    assertThat(result.get(0).restoredArticleIds())
        .containsExactly(savedArticles.get(0).getId(), savedArticles.get(1).getId());
  }

  @Test
  @DisplayName("성공 - 이미 존재하는 기사 URL은 제외하고 신규 기사만 복구한다")
  void restore_success_excludeExistingSourceUrl() throws Exception {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);
    byte[] json = "{}".getBytes();
    ArticleBackupDto existing = backupDto("https://news.monew.test/existing", "기존 기사", 1, 10);
    ArticleBackupDto fresh = backupDto("https://news.monew.test/fresh", "신규 기사", 2, 20);

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(true);
    given(articleBackupReader.download(KEY_2026_07_01)).willReturn(json);
    given(objectMapper.readValue(eq(json), anyBackupDtoListType()))
        .willReturn(List.of(existing, fresh));
    given(articleRepository.findExistingSourceUrls(List.of(existing.sourceUrl(), fresh.sourceUrl())))
        .willReturn(Set.of(existing.sourceUrl()));
    given(articleRepository.saveAll(any())).willAnswer(invocation -> toList(invocation.getArgument(0)));

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    ArgumentCaptor<Iterable<Article>> captor = ArgumentCaptor.forClass(Iterable.class);
    then(articleRepository).should().saveAll(captor.capture());
    List<Article> savedArticles = toList(captor.getValue());

    assertThat(savedArticles).hasSize(1);
    assertThat(savedArticles.get(0).getSourceUrl()).isEqualTo(fresh.sourceUrl());
    assertThat(result.get(0).restoredArticleCount()).isEqualTo(1);
    assertThat(result.get(0).restoredArticleIds()).containsExactly(savedArticles.get(0).getId());
  }

  @Test
  @DisplayName("성공 - 백업 내부에 같은 URL이 있으면 첫 번째 기사만 복구한다")
  void restore_success_deduplicateBackupCandidatesBySourceUrl() throws Exception {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);
    byte[] json = "{}".getBytes();
    String sourceUrl = "https://news.monew.test/duplicate";
    ArticleBackupDto first = backupDto(sourceUrl, "첫 번째 중복 기사", "첫 번째 요약");
    ArticleBackupDto second = backupDto(sourceUrl, "두 번째 중복 기사", "두 번째 요약");

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(true);
    given(articleBackupReader.download(KEY_2026_07_01)).willReturn(json);
    given(objectMapper.readValue(eq(json), anyBackupDtoListType()))
        .willReturn(List.of(first, second));
    given(articleRepository.findExistingSourceUrls(List.of(sourceUrl))).willReturn(Set.of());
    given(articleRepository.saveAll(any())).willAnswer(invocation -> toList(invocation.getArgument(0)));

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    ArgumentCaptor<Iterable<Article>> captor = ArgumentCaptor.forClass(Iterable.class);
    then(articleRepository).should().saveAll(captor.capture());
    List<Article> savedArticles = toList(captor.getValue());

    assertThat(savedArticles).hasSize(1);
    assertThat(savedArticles.get(0).getTitle()).isEqualTo(first.title());
    assertThat(savedArticles.get(0).getSummary()).isEqualTo(first.summary());
    assertThat(result.get(0).restoredArticleCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공 - 복구 불가능한 후보는 제외하고 유효한 기사만 복구한다")
  void restore_success_filterUnrestorableCandidates() throws Exception {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);
    byte[] json = "{}".getBytes();
    ArticleBackupDto valid = backupDto("https://news.monew.test/valid", "유효한 기사", 1, 10);
    ArticleBackupDto nullSourceUrl = backupDto(null, "URL이 없는 기사", 1, 10);
    ArticleBackupDto blankSourceUrl = backupDto("   ", "URL이 공백인 기사", 1, 10);
    ArticleBackupDto nullPublishDate = backupDto(
        ArticleSource.NAVER,
        "https://news.monew.test/no-publish-date",
        "발행일이 없는 기사",
        null,
        "요약",
        1,
        10,
        null
    );
    ArticleBackupDto deleted = backupDto(
        ArticleSource.NAVER,
        "https://news.monew.test/deleted",
        "삭제된 기사",
        LocalDateTime.of(2026, 7, 1, 9, 0),
        "요약",
        1,
        10,
        LocalDateTime.of(2026, 7, 1, 11, 0)
    );

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(true);
    given(articleBackupReader.download(KEY_2026_07_01)).willReturn(json);
    given(objectMapper.readValue(eq(json), anyBackupDtoListType()))
        .willReturn(Arrays.asList(null, nullSourceUrl, blankSourceUrl, nullPublishDate, deleted, valid));
    given(articleRepository.findExistingSourceUrls(List.of(valid.sourceUrl()))).willReturn(Set.of());
    given(articleRepository.saveAll(any())).willAnswer(invocation -> toList(invocation.getArgument(0)));

    // when
    List<ArticleRestoreResultDto> result = articleRestoreService.restore(from, to);

    // then
    ArgumentCaptor<Iterable<Article>> captor = ArgumentCaptor.forClass(Iterable.class);
    then(articleRepository).should().saveAll(captor.capture());
    List<Article> savedArticles = toList(captor.getValue());

    assertThat(savedArticles).hasSize(1);
    assertThat(savedArticles.get(0).getSourceUrl()).isEqualTo(valid.sourceUrl());
    assertThat(result.get(0).restoredArticleCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("실패 - 백업 JSON 파싱에 실패하면 백업 파싱 실패 예외가 발생한다")
  void restore_fail_jsonParseFailed() throws Exception {
    // given
    LocalDateTime from = LocalDateTime.of(2026, 7, 1, 10, 0);
    LocalDateTime to = LocalDateTime.of(2026, 7, 2, 0, 0);
    byte[] json = "{".getBytes();

    given(articleBackupReader.exists(KEY_2026_07_01)).willReturn(true);
    given(articleBackupReader.download(KEY_2026_07_01)).willReturn(json);
    given(objectMapper.readValue(eq(json), anyBackupDtoListType()))
        .willThrow(new IOException("json parse failed"));

    // when & then
    assertThatThrownBy(() -> articleRestoreService.restore(from, to))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.ARTICLE_BACKUP_JSON_PARSE_FAILED);

    then(articleRepository).should(never()).saveAll(any());
  }

  @SuppressWarnings("unchecked")
  private TypeReference<List<ArticleBackupDto>> anyBackupDtoListType() {
    return any(TypeReference.class);
  }

  private ArticleBackupDto backupDto(String sourceUrl, String title, long commentCount, long viewCount) {
    return backupDto(sourceUrl, title, "기사 요약", commentCount, viewCount);
  }

  private ArticleBackupDto backupDto(String sourceUrl, String title, String summary) {
    return backupDto(sourceUrl, title, summary, 1, 10);
  }

  private ArticleBackupDto backupDto(
      String sourceUrl,
      String title,
      String summary,
      long commentCount,
      long viewCount
  ) {
    return backupDto(
        ArticleSource.NAVER,
        sourceUrl,
        title,
        LocalDateTime.of(2026, 7, 1, 9, 0),
        summary,
        commentCount,
        viewCount,
        null
    );
  }

  private ArticleBackupDto backupDto(
      ArticleSource source,
      String sourceUrl,
      String title,
      LocalDateTime publishDate,
      String summary,
      long commentCount,
      long viewCount,
      LocalDateTime deletedAt
  ) {
    return new ArticleBackupDto(
        source,
        sourceUrl,
        title,
        publishDate,
        summary,
        commentCount,
        viewCount,
        publishDate == null ? null : publishDate.plusMinutes(1),
        publishDate == null ? null : publishDate.plusMinutes(2),
        deletedAt
    );
  }

  private List<Article> toList(Iterable<Article> articles) {
    List<Article> result = new ArrayList<>();
    articles.forEach(result::add);
    return result;
  }
}
