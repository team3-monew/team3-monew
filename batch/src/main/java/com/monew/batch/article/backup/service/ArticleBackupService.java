package com.monew.batch.article.backup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.batch.article.backup.config.BackupProperties;
import com.monew.batch.article.backup.dto.ArticleBackupDto;
import com.monew.batch.article.backup.exception.ArticleBackupException;
import com.monew.batch.article.backup.repository.ArticleBackupRepository;
import com.monew.batch.article.backup.storage.ArticleBackupStorage;
import com.monew.batch.article.entity.Article;
import com.monew.batch.article.entity.ArticleBackup;
import com.monew.batch.article.repository.ArticleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 기사 백업의 실제 업무 흐름을 담당합니다.
 * 날짜 기준 기사 조회, DTO 변환, JSON 직렬화, 저장소 업로드, 백업 이력 상태 갱신을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleBackupService {

  private final ArticleRepository articleRepository;
  private final ArticleBackupRepository articleBackupRepository;
  private final ArticleBackupStorage articleBackupStorage;
  private final BackupProperties backupProperties;
  private final ObjectMapper objectMapper;
  private final PlatformTransactionManager transactionManager;

  // 하루 동안 발행된 기사들을 JSON으로 만들어 설정된 저장소에 업로드합니다.
  public void backup(LocalDate targetDate) {
    String storageKey = storageKey(targetDate);
    ArticleBackup backupHistory = recordRunning(targetDate, storageKey);

    try {
      LocalDateTime start = targetDate.atStartOfDay();
      LocalDateTime end = targetDate.plusDays(1).atStartOfDay();
      // 전날 발행된 기사들 조회
      List<Article> articles =
          articleRepository.findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(start, end);
      log.info("[article backup]백업할 기사 조회 성공. 백업 날짜={}, 백업할 기사 수={}",
          targetDate, articles.size());

      List<ArticleBackupDto> payload = articles.stream()
          .map(ArticleBackupDto::from)
          .toList();
      byte[] json = toJson(payload);

      // 기사 백업
      articleBackupStorage.upload(storageKey, json);

      recordSuccess(backupHistory.getId(), articles.size());
      log.info("[article backup] 뉴스 기사 백업 성공. 백업 날짜={}, 저장소 key={}, 백업한 기사 수={}",
          targetDate, storageKey, articles.size());

    } catch (Exception ex) {
      recordFailure(backupHistory.getId());
      log.error("[article backup] 뉴스 기사 백업 실패. 백업 날짜={}, 저장소 key={}, message={}",
          targetDate, storageKey, ex.getMessage(), ex);

      throw ex;
    }
  }

  private byte[] toJson(List<ArticleBackupDto> payload) {
    try {
      return objectMapper.writeValueAsBytes(payload);
    } catch (JsonProcessingException ex) {
      throw new ArticleBackupException("Failed to serialize article backup JSON", ex);
    }
  }

  // 별도 트랜잭션으로 백업 시작 상태 이력 기록
  private ArticleBackup recordRunning(LocalDate targetDate, String storageKey) {
    return newHistoryTransaction().execute(status -> {
      ArticleBackup backupHistory = articleBackupRepository.findByBackupDate(targetDate)
          .map(backup -> {
            backup.start(articleBackupStorage.bucketName(), storageKey);
            return backup;
          })
          .orElseGet(() -> ArticleBackup.running(targetDate, articleBackupStorage.bucketName(),
              storageKey));
      return articleBackupRepository.saveAndFlush(backupHistory);
    });
  }

  // 별도 트랜잭션으로 백업 성공 상태와 기사 수 기록
  private void recordSuccess(java.util.UUID backupId, long articleCount) {
    newHistoryTransaction().executeWithoutResult(status -> {
      ArticleBackup backupHistory = articleBackupRepository.findById(backupId)
          .orElseThrow(() -> new ArticleBackupException("Article backup history not found"));
      backupHistory.succeed(articleCount);
      articleBackupRepository.saveAndFlush(backupHistory);
    });
  }

  // 별도 트랜잭션으로 백업 실패 상태 기록
  private void recordFailure(java.util.UUID backupId) {
    newHistoryTransaction().executeWithoutResult(status -> {
      ArticleBackup backupHistory = articleBackupRepository.findById(backupId)
          .orElseThrow(() -> new ArticleBackupException("Article backup history not found"));
      backupHistory.fail();
      articleBackupRepository.saveAndFlush(backupHistory);
    });
  }

  /**
   * Step 트랜잭션 롤백과 별개로 이력 저장을 커밋하기 위한 REQUIRES_NEW 트랜잭션 템플릿입니다.
   */
  private TransactionTemplate newHistoryTransaction() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transactionTemplate;
  }


  // 날짜별 백업 key 형식 생성
  private String storageKey(LocalDate targetDate) {
    String prefix = backupProperties.storage().s3().prefix();
    String normalizedPrefix = prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    return "%s/date=%s/articles-%s.json".formatted(normalizedPrefix, targetDate, targetDate);
  }
}
