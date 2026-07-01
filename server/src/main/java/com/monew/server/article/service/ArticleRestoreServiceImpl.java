package com.monew.server.article.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.server.article.config.BackupProperties;
import com.monew.server.article.dto.ArticleBackupDto;
import com.monew.server.article.dto.ArticleRestoreResultDto;
import com.monew.server.article.entity.Article;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.article.storage.ArticleBackupReader;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.article.ArticleException;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRestoreServiceImpl implements ArticleRestoreService {

  private static final TypeReference<List<ArticleBackupDto>> BACKUP_DTO_LIST_TYPE =
      new TypeReference<>() {
      };

  private final ArticleRepository articleRepository;
  private final ArticleBackupReader articleBackupReader;
  private final BackupProperties backupProperties;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public List<ArticleRestoreResultDto> restore(LocalDateTime from, LocalDateTime to) {
    validateDateRange(from, to);

    List<LocalDate> restoreDates = restoreDates(from, to);  // 복구 날짜 리스트 추출
    log.info("[article restore] 복구 날짜 리스트={}", restoreDates);

    // 복구 기사 후보 추출
    List<ArticleBackupDto> restoreCandidates = restoreDates.stream()
        .flatMap(date -> readBackups(date).stream())
        .filter(this::isRestorable)
        .toList();

    Set<String> existingSourceUrls = findExistingSourceUrls(restoreCandidates);
    log.info("[article restore] 기존 뉴스 기사 개수={}", existingSourceUrls.size());

    Map<String, ArticleBackupDto> uniqueCandidates = new LinkedHashMap<>();
    for (ArticleBackupDto dto : restoreCandidates) {
      if (!existingSourceUrls.contains(dto.sourceUrl())) {
        uniqueCandidates.putIfAbsent(dto.sourceUrl(), dto);
      }
    }

    List<Article> restoredArticles = uniqueCandidates.values().stream()
        .map(Article::fromBackup)
        .toList();
    List<Article> savedArticles = articleRepository.saveAll(restoredArticles);
    List<UUID> restoredArticleIds = savedArticles.stream()
        .map(Article::getId)
        .toList();

    return List.of(new ArticleRestoreResultDto(
        Instant.now(),
        restoredArticleIds,
        restoredArticleIds.size()
    ));
  }

  private void validateDateRange(LocalDateTime from, LocalDateTime to) {
    if (from == null) {
      throwInvalidRequest("from", null);
    }

    if (to == null) {
      throwInvalidRequest("to", null);
    }

    if (!from.isBefore(to)) {
      ArticleException exception =
          new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
      exception.addDetail("from", from);
      exception.addDetail("to", to);
      throw exception;
    }
  }

  private List<LocalDate> restoreDates(LocalDateTime from, LocalDateTime to) {
    LocalDate startDate = from.toLocalDate();
    LocalDate endExclusive = to.toLocalDate();

    if (!to.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      endExclusive = endExclusive.plusDays(1);
    }

    return startDate.datesUntil(endExclusive).toList();
  }

  // 백업 저장소에 기사 읽어오는 메서드
  private List<ArticleBackupDto> readBackups(LocalDate date) {
    String key = storageKey(date);
    boolean exists = articleBackupReader.exists(key);
    log.info("[article restore] 백업 파일 체크. date={}, key={}, exists={}",
        date, key, exists);

    if (!exists) {
      return List.of();
    }

    byte[] json = articleBackupReader.download(key);
    try {
      // json -> dto 변경
      List<ArticleBackupDto> articles = objectMapper.readValue(json, BACKUP_DTO_LIST_TYPE);
      return articles;

    } catch (IOException ex) {
      ArticleException exception =
          new ArticleException(ArticleErrorCode.ARTICLE_BACKUP_JSON_PARSE_FAILED, ex);
      exception.addDetail("key", key);

      throw exception;
    }
  }

  private boolean isRestorable(ArticleBackupDto dto) {
    return dto != null
        && dto.sourceUrl() != null
        && !dto.sourceUrl().isBlank()
        && dto.publishDate() != null
        && dto.deletedAt() == null;
  }

  private Set<String> findExistingSourceUrls(Collection<ArticleBackupDto> candidates) {
    List<String> sourceUrls = candidates.stream()
        .map(ArticleBackupDto::sourceUrl)
        .filter(Objects::nonNull)
        .filter(sourceUrl -> !sourceUrl.isBlank())
        .distinct()
        .toList();

    if (sourceUrls.isEmpty()) {
      return Set.of();
    }

    return articleRepository.findExistingSourceUrls(sourceUrls);
  }

  private String storageKey(LocalDate targetDate) {
    String prefix = backupProperties.storage().s3().prefix();
    String normalizedPrefix = prefix.replaceAll("^/+", "").replaceAll("/+$", "");

    return "%s/date=%s/articles-%s.json".formatted(normalizedPrefix, targetDate, targetDate);
  }

  private void throwInvalidRequest(String fieldName, Object value) {
    ArticleException exception =
        new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    exception.addDetail(fieldName, value);

    throw exception;
  }

}
