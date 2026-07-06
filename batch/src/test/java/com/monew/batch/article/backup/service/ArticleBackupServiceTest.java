package com.monew.batch.article.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.batch.article.backup.config.BackupProperties;
import com.monew.batch.article.backup.dto.ArticleBackupResultDto;
import com.monew.batch.article.backup.repository.ArticleBackupRepository;
import com.monew.batch.article.backup.storage.ArticleBackupStorage;
import com.monew.batch.article.entity.Article;
import com.monew.batch.article.entity.ArticleBackup;
import com.monew.batch.article.entity.ArticleBackupStatus;
import com.monew.batch.article.entity.ArticleSource;
import com.monew.batch.article.repository.ArticleRepository;
import com.monew.batch.common.exception.article.ArticleBackupErrorCode;
import com.monew.batch.common.exception.article.ArticleBackupException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleBackupServiceTest {

  private static final LocalDate TARGET_DATE = LocalDate.of(2026, 6, 29);
  private static final String STORAGE_KEY =
      "backups/articles/date=2026-06-29/articles-2026-06-29.json";

  @Mock
  private ArticleRepository articleRepository;

  @Mock
  private ArticleBackupRepository articleBackupRepository;

  @Mock
  private ArticleBackupStorage articleBackupStorage;

  @Mock
  private ObjectMapper objectMapper;

  private final Map<UUID, ArticleBackup> savedBackups = new HashMap<>();

  @BeforeEach
  void setUp() {
    savedBackups.clear();

    given(articleBackupStorage.bucketName()).willReturn("local");
    given(articleBackupRepository.saveAndFlush(any(ArticleBackup.class)))
        .willAnswer(invocation -> {
          ArticleBackup backup = invocation.getArgument(0);
          if (backup.getId() == null) {
            ReflectionTestUtils.setField(backup, "id", UUID.randomUUID());
          }
          savedBackups.put(backup.getId(), backup);
          return backup;
        });
    lenient().when(articleBackupRepository.findById(any(UUID.class)))
        .thenAnswer(invocation -> Optional.ofNullable(savedBackups.get(invocation.getArgument(0))));
  }

  @Test
  @DisplayName("기사 백업 성공 - 대상 날짜 기사 2건을 JSON으로 업로드하고 이력을 SUCCESS로 기록한다")
  void backup_success() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    List<Article> articles = List.of(
        article("https://news.example.com/1", "첫 번째 기사", TARGET_DATE.atTime(10, 0)),
        article("https://news.example.com/2", "두 번째 기사", TARGET_DATE.atTime(18, 30))
    );
    byte[] json = "[{\"title\":\"첫 번째 기사\"},{\"title\":\"두 번째 기사\"}]"
        .getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(articles);
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when
    ArticleBackupResultDto result = service.backup(TARGET_DATE);

    // then
    verify(articleRepository).findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay());
    verify(articleBackupStorage).upload(STORAGE_KEY, json);

    ArticleBackup backup = onlySavedBackup();
    assertThat(backup.getStatus()).isEqualTo(ArticleBackupStatus.SUCCESS);
    assertThat(backup.getArticleCount()).isEqualTo(2);
    assertThat(backup.getBackupDate()).isEqualTo(TARGET_DATE);
    assertThat(backup.getS3Bucket()).isEqualTo("local");
    assertThat(backup.getS3ObjectKey()).isEqualTo(STORAGE_KEY);

    assertThat(result.targetCount()).isEqualTo(2);
    assertThat(result.successCount()).isEqualTo(2);
    assertThat(result.failureCount()).isZero();
    assertThat(result.fileCount()).isEqualTo(1);
    assertThat(result.fileSizeBytes()).isEqualTo(json.length);
  }

  @Test
  @DisplayName("기사 백업 성공 - 백업 대상 기사가 0건이어도 빈 JSON 배열을 업로드하고 SUCCESS로 기록한다")
  void backup_successWithEmptyArticles() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    byte[] json = "[]".getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of());
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when
    ArticleBackupResultDto result = service.backup(TARGET_DATE);

    // then
    verify(articleBackupStorage).upload(STORAGE_KEY, json);

    ArticleBackup backup = onlySavedBackup();
    assertThat(backup.getStatus()).isEqualTo(ArticleBackupStatus.SUCCESS);
    assertThat(backup.getArticleCount()).isZero();

    assertThat(result.targetCount()).isZero();
    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isZero();
    assertThat(result.fileCount()).isEqualTo(1);
    assertThat(result.fileSizeBytes()).isEqualTo(json.length);
  }

  @Test
  @DisplayName("기사 백업 조회 - targetDate 시작 시각 이상, 다음날 시작 시각 미만 범위로 기사를 조회한다")
  void backup_usesTargetDateRange() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    LocalDateTime start = TARGET_DATE.atStartOfDay();
    LocalDateTime end = TARGET_DATE.plusDays(1).atStartOfDay();

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(start, end))
        .willReturn(List.of(
            article("https://news.example.com/start", "시작 경계 기사", start),
            article("https://news.example.com/end-before", "종료 직전 기사",
                TARGET_DATE.atTime(23, 59, 59))
        ));
    given(objectMapper.writeValueAsBytes(any())).willReturn("[]".getBytes(StandardCharsets.UTF_8));

    // when
    service.backup(TARGET_DATE);

    // then
    verify(articleRepository).findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(start,
        end);
  }

  @Test
  @DisplayName("저장소 key 생성 - 기본 prefix와 백업 날짜로 날짜별 JSON 경로를 생성한다")
  void backup_createsStorageKey() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    byte[] json = "[]".getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of());
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when
    service.backup(TARGET_DATE);

    // then
    verify(articleBackupStorage).upload(STORAGE_KEY, json);
  }

  @Test
  @DisplayName("저장소 key 생성 - prefix 앞뒤 슬래시를 제거하고 날짜별 JSON 경로를 생성한다")
  void backup_normalizesStorageKeyPrefix() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("/backups/articles/");
    byte[] json = "[]".getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of());
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when
    service.backup(TARGET_DATE);

    // then
    verify(articleBackupStorage).upload(STORAGE_KEY, json);
  }

  @Test
  @DisplayName("기사 백업 재실행 - 기존 이력을 새 row로 만들지 않고 RUNNING에서 SUCCESS로 갱신한다")
  void backup_updatesExistingBackupHistory() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    ArticleBackup existing = ArticleBackup.running(TARGET_DATE, "old-bucket", "old-key");
    ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
    existing.succeed(10);
    savedBackups.put(existing.getId(), existing);

    List<Article> articles = List.of(
        article("https://news.example.com/retry", "재실행 기사", TARGET_DATE.atTime(9, 0)));
    byte[] json = "[{\"title\":\"재실행 기사\"}]".getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.of(existing));
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(articles);
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when
    service.backup(TARGET_DATE);

    // then
    assertThat(savedBackups).hasSize(1);
    assertThat(existing.getStatus()).isEqualTo(ArticleBackupStatus.SUCCESS);
    assertThat(existing.getArticleCount()).isEqualTo(1);
    assertThat(existing.getS3Bucket()).isEqualTo("local");
    assertThat(existing.getS3ObjectKey()).isEqualTo(STORAGE_KEY);
  }

  @Test
  @DisplayName("기사 백업 실패 - 저장소 업로드가 실패하면 이력을 FAILED로 기록하고 예외를 전파한다")
  void backup_failsWhenStorageUploadFails() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    RuntimeException uploadException = new RuntimeException("upload failed");

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of(article("https://news.example.com/1", "업로드 실패 기사",
            TARGET_DATE.atTime(10, 0))));
    given(objectMapper.writeValueAsBytes(any())).willReturn("[]".getBytes(StandardCharsets.UTF_8));
    doThrow(uploadException).when(articleBackupStorage).upload(eq(STORAGE_KEY), any(byte[].class));

    // when & then
    assertThatThrownBy(() -> service.backup(TARGET_DATE))
        .isSameAs(uploadException);

    ArticleBackup backup = onlySavedBackup();
    assertThat(backup.getStatus()).isEqualTo(ArticleBackupStatus.FAILED);
  }

  @Test
  @DisplayName("기사 백업 실패 - JSON 직렬화 실패 시 BACKUP_JSON_SERIALIZE_FAILED 예외와 FAILED 이력을 남긴다")
  void backup_failsWhenJsonSerializationFails() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of(article("https://news.example.com/1", "직렬화 실패 기사",
            TARGET_DATE.atTime(10, 0))));
    given(objectMapper.writeValueAsBytes(any()))
        .willThrow(new JsonProcessingException("serialize failed") {
        });

    // when & then
    assertThatThrownBy(() -> service.backup(TARGET_DATE))
        .isInstanceOf(ArticleBackupException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleBackupErrorCode.BACKUP_JSON_SERIALIZE_FAILED);

    ArticleBackup backup = onlySavedBackup();
    assertThat(backup.getStatus()).isEqualTo(ArticleBackupStatus.FAILED);
    verify(articleBackupStorage, never()).upload(any(), any());
  }

  @Test
  @DisplayName("기사 백업 실패 - 성공 기록 대상 이력이 사라지면 BACKUP_HISTORY_NOT_FOUND 예외를 던진다")
  void backup_failsWhenSuccessHistoryNotFound() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    byte[] json = "[]".getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleBackupRepository.findById(any(UUID.class))).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of());
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when & then
    assertThatThrownBy(() -> service.backup(TARGET_DATE))
        .isInstanceOf(ArticleBackupException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleBackupErrorCode.BACKUP_HISTORY_NOT_FOUND);
    verify(articleBackupStorage).upload(STORAGE_KEY, json);
  }

  @Test
  @DisplayName("기사 백업 실패 - 실패 기록 대상 이력이 사라지면 BACKUP_HISTORY_NOT_FOUND 예외를 던진다")
  void backup_failsWhenFailureHistoryNotFound() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    RuntimeException uploadException = new RuntimeException("upload failed");

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleBackupRepository.findById(any(UUID.class))).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(List.of());
    given(objectMapper.writeValueAsBytes(any())).willReturn("[]".getBytes(StandardCharsets.UTF_8));
    doThrow(uploadException).when(articleBackupStorage).upload(eq(STORAGE_KEY), any(byte[].class));

    // when & then
    assertThatThrownBy(() -> service.backup(TARGET_DATE))
        .isInstanceOf(ArticleBackupException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleBackupErrorCode.BACKUP_HISTORY_NOT_FOUND);
  }

  @Test
  @DisplayName("기사 백업 JSON 변환 - 조회한 기사들을 백업 DTO 목록으로 변환해 ObjectMapper에 전달한다")
  void backup_convertsArticlesToBackupDtos() throws Exception {
    // given
    ArticleBackupService service = serviceWithPrefix("backups/articles");
    List<Article> articles = List.of(
        article("https://news.example.com/1", "DTO 변환 기사", TARGET_DATE.atTime(11, 0)));
    byte[] json = "[{\"title\":\"DTO 변환 기사\"}]".getBytes(StandardCharsets.UTF_8);

    given(articleBackupRepository.findByBackupDate(TARGET_DATE)).willReturn(Optional.empty());
    given(articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
        TARGET_DATE.atStartOfDay(), TARGET_DATE.plusDays(1).atStartOfDay()))
        .willReturn(articles);
    given(objectMapper.writeValueAsBytes(any())).willReturn(json);

    // when
    service.backup(TARGET_DATE);

    // then
    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    verify(objectMapper).writeValueAsBytes(payloadCaptor.capture());
    assertThat((List<?>) payloadCaptor.getValue()).hasSize(1);
  }

  private ArticleBackupService serviceWithPrefix(String prefix) {
    BackupProperties properties = new BackupProperties(true,
        new BackupProperties.Storage("local",
            new BackupProperties.Local(Path.of(".monew/backups")),
            new BackupProperties.S3("", prefix, "ap-northeast-2")));

    return new ArticleBackupService(
        articleRepository,
        articleBackupRepository,
        articleBackupStorage,
        properties,
        objectMapper,
        new ResourcelessTransactionManager()
    );
  }

  private Article article(String sourceUrl, String title, LocalDateTime publishDate) {
    return new Article(ArticleSource.NAVER, sourceUrl, title, publishDate, "요약");
  }

  private ArticleBackup onlySavedBackup() {
    assertThat(savedBackups).hasSize(1);
    return savedBackups.values().iterator().next();
  }
}
