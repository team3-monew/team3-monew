package com.monew.batch.article.collect.config;

import com.monew.batch.article.collect.dto.ArticleCollectResultDto;
import com.monew.batch.article.collect.dto.ArticleCollectStagingCleanupResultDto;
import com.monew.batch.article.collect.dto.ArticleCollectStepResultDto;
import com.monew.batch.article.collect.dto.ArticleSaveAndInterestLinkStepResultDto;
import com.monew.batch.article.collect.service.ArticleCollectService;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.entity.ArticleSource;
import com.monew.batch.monitoring.ArticleCollectMetrics;
import com.monew.batch.monitoring.BatchJobMetricsListener;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.sql.SQLTransientException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionSystemException;

/**
 * 기사 수집 작업을 Spring Batch Job과 Step으로 등록하는 설정 클래스입니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArticleCollectJobConfig {

  private static final String NAVER_REQUEST_COUNT = "naverRequestCount";
  private static final String NAVER_SUCCESS_COUNT = "naverSuccessCount";
  private static final String NAVER_FAILURE_COUNT = "naverFailureCount";
  private static final String RSS_REQUEST_COUNT = "rssRequestCount";
  private static final String RSS_SUCCESS_COUNT = "rssSuccessCount";
  private static final String RSS_FAILURE_COUNT = "rssFailureCount";
  private static final String YEONHAP_RSS_REQUEST_COUNT = "yeonhapRssRequestCount";
  private static final String YEONHAP_RSS_SUCCESS_COUNT = "yeonhapRssSuccessCount";
  private static final String YEONHAP_RSS_FAILURE_COUNT = "yeonhapRssFailureCount";
  private static final String COLLECTED_COUNT = "collectedCount";
  private static final String SAVED_COUNT = "savedCount";
  private static final String DUPLICATE_SKIPPED_COUNT = "duplicateSkippedCount";
  private static final String INVALID_SKIPPED_COUNT = "invalidSkippedCount";
  private static final ExitStatus COMPLETED_WITH_EXTERNAL_API_FAILURE =
      new ExitStatus("COMPLETED_WITH_EXTERNAL_API_FAILURE");

  private final ArticleCollectService articleCollectService;
  private final ArticleCollectMetrics articleCollectMetrics;
  private final ArticleCollectProperties articleCollectProperties;

  /**
   * 매 시간 실행될 기사 수집 Batch Job입니다.
   * 외부 수집 Step은 실패해도 다음 출처 수집으로 넘어가고,
   * DB 반영 Step과 staging 정리 Step은 실패 시 재시도 후 최종 실패하면 Job을 실패시킵니다.
   */
  @Bean
  public Job articleCollectJob(JobRepository jobRepository,
      Step naverCollectStep,
      Step hankyungRssCollectStep,
      Step chosunRssCollectStep,
      Step yeonhapRssCollectStep,
      Step articlePersistStep,
      Step articleCollectStagingCleanupStep,
      BatchJobMetricsListener batchJobMetricsListener) {
    return new JobBuilder("articleCollectJob", jobRepository)
        // Job 종료 시 공통 실행/성공/실패/소요시간 메트릭을 기록합니다.
        .listener(batchJobMetricsListener)
        .start(naverCollectStep)
        .next(hankyungRssCollectStep)
        .next(chosunRssCollectStep)
        .next(yeonhapRssCollectStep)
        .next(articlePersistStep)
        .next(articleCollectStagingCleanupStep)
        .build();
  }

  @Bean
  public Step naverCollectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return collectStep(jobRepository, transactionManager, "naverCollectStep", chunkContext ->
        articleCollectService.collectNaverArticlesToStaging(jobInstanceId(chunkContext)));
  }

  @Bean
  public Step hankyungRssCollectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return rssCollectStep(jobRepository, transactionManager, "hankyungRssCollectStep",
        ArticleSource.HANKYUNG);
  }

  @Bean
  public Step chosunRssCollectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return rssCollectStep(jobRepository, transactionManager, "chosunRssCollectStep",
        ArticleSource.CHOSUN);
  }

  @Bean
  public Step yeonhapRssCollectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return rssCollectStep(jobRepository, transactionManager, "yeonhapRssCollectStep",
        ArticleSource.YEONHAP);
  }

  /**
   * staging 데이터를 articles에 저장하고, 저장된 기사와 관심사를 연결하는 Step입니다.
   * DB 반영 작업이므로 실패 예외를 삼키지 않고 그대로 던져 Step이 FAILED로 기록되게 합니다.
   * 같은 JobInstance를 재시작하면 완료된 수집 Step은 건너뛰고 이 Step부터 다시 실행될 수 있습니다.
   */
  @Bean
  public Step articlePersistStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("articlePersistStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          ArticleSaveAndInterestLinkStepResultDto result =
              executeDbStepWithRetry("articlePersistStep",
                  () -> articleCollectService.saveStagedArticlesAndLinkInterests(
                      jobInstanceId(chunkContext)));
          // staging 데이터를 실제 articles에 반영한 뒤 최종 처리 건수 메트릭을 갱신합니다.
          articleCollectMetrics.recordPersistResult(result);

          ExecutionContext context = jobContext(chunkContext);
          context.putInt(SAVED_COUNT, result.savedCount());
          increment(context, DUPLICATE_SKIPPED_COUNT, result.duplicateSkippedCount());
          context.putInt(INVALID_SKIPPED_COUNT, result.invalidSkippedCount());

          ArticleCollectResultDto finalResult = toFinalResult(context,
              result.articleInterestLinkedCount());
          log.info("Article collect job finished before cleanup. result={}", finalResult);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  /**
   * articlePersistStep이 성공한 뒤 이번 실행에서 사용한 staging 데이터를 삭제하는 Step입니다.
   * cleanup 실패도 예외를 삼키지 않고 그대로 던져 Step이 FAILED로 기록되게 합니다.
   * 재시작 시 articlePersistStep이 이미 COMPLETED라면 이 cleanup Step부터 다시 실행될 수 있습니다.
   */
  @Bean
  public Step articleCollectStagingCleanupStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("articleCollectStagingCleanupStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          ArticleCollectStagingCleanupResultDto result =
              executeDbStepWithRetry("articleCollectStagingCleanupStep",
                  () -> articleCollectService.cleanupStaging(jobInstanceId(chunkContext)));
          log.info("Article collect staging cleanup finished. result={}", result);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  /**
   * RSS 출처별 수집 Step을 같은 실패 정책으로 만들기 위한 공통 생성 메서드입니다.
   */
  private Step rssCollectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      String stepName,
      ArticleSource source) {
    return collectStep(jobRepository, transactionManager, stepName, chunkContext ->
        articleCollectService.collectRssArticlesToStaging(jobInstanceId(chunkContext), source));
  }

  /**
   * 외부 수집 Step을 생성합니다.
   * Step 실행 중 예외가 나도 로그만 남기고 FINISHED를 반환해서 다음 Step으로 계속 진행합니다.
   */
  private Step collectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      String stepName,
      Function<ChunkContext, ArticleCollectStepResultDto> collectAction) {
    return new StepBuilder(stepName, jobRepository)
        .tasklet((contribution, chunkContext) -> {
          try {
            ArticleCollectStepResultDto result = collectAction.apply(chunkContext);
            recordCollectResult(jobContext(chunkContext), result);

            // 각 수집 Step이 끝날 때 source별 staging 결과를 먼저 기록합니다.
            articleCollectMetrics.recordSourceCollectResult(result);

            if (result.failureCount() > 0) {
              contribution.setExitStatus(COMPLETED_WITH_EXTERNAL_API_FAILURE);
            }
          } catch (Exception ex) {
            log.warn("[collect article] 기사 수집 step 실패. 다음 step 실행. stepName={}, message={}",
                stepName, ex.getMessage(), ex);
            contribution.setExitStatus(COMPLETED_WITH_EXTERNAL_API_FAILURE);
          }
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  /**
   * 현재 JobInstance ID를 꺼냅니다.
   * 이 ID를 staging row에 함께 저장해서 재시작 JobExecution도 같은 수집 데이터를 읽게 합니다.
   */
  private Long jobInstanceId(ChunkContext chunkContext) {
    return chunkContext.getStepContext()
        .getStepExecution()
        .getJobExecution()
        .getJobInstance()
        .getInstanceId();
  }

  /**
   * DB 저장/정리 Step에서 일시적인 DB 장애가 발생하면 설정된 횟수만큼 재시도합니다.
   * 재시도 대상이 아니거나 최대 시도 횟수를 넘긴 예외는 그대로 던져 Step과 Job을 실패 상태로 남깁니다.
   */
  private <T> T executeDbStepWithRetry(String stepName, Supplier<T> action) {
    int maxAttempts = Math.max(1, articleCollectProperties.retryMaxAttempts());

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return action.get();
      } catch (RuntimeException ex) {
        if (!isRetryableDbException(ex) || attempt == maxAttempts) {
          log.warn("[collect article] 기사 수집 DB 스텝 실패. stepName={}, attempt={}, message={}",
              stepName, attempt, ex.getMessage(), ex);
          throw ex;
        }

        log.warn("[collect article] 재시도 가능한 기사 수집 DB step 실패. stepName={}, attempt={}, message={}",
            stepName, attempt, ex.getMessage(), ex);
        sleepBeforeRetry(attempt);
      }
    }

    throw new IllegalStateException("Unexpected DB retry state. stepName=" + stepName);
  }

  private boolean isRetryableDbException(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof TransientDataAccessException
          || current instanceof SQLTransientException
          || current instanceof JDBCConnectionException
          || current instanceof LockAcquisitionException
          || current instanceof LockTimeoutException
          || current instanceof PessimisticLockException
          || current instanceof TransactionSystemException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private void sleepBeforeRetry(int failedAttempt) {
    long baseDelay = articleCollectProperties.retryInitialDelayMillis()
        * (1L << Math.max(0, failedAttempt - 1));
    long cappedDelay = Math.min(baseDelay, articleCollectProperties.retryMaxDelayMillis());
    long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1L, cappedDelay / 5L + 1L));

    try {
      Thread.sleep(cappedDelay + jitter);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for DB retry.", ex);
    }
  }

  /**
   * Step 간에 공유되는 Job ExecutionContext를 꺼냅니다.
   * 수집/저장 결과 카운트를 여기에 모아 최종 결과 DTO를 만듭니다.
   */
  private ExecutionContext jobContext(ChunkContext chunkContext) {
    return chunkContext.getStepContext()
        .getStepExecution()
        .getJobExecution()
        .getExecutionContext();
  }

  /**
   * 수집 Step 결과를 Job ExecutionContext에 누적합니다.
   * Job이 끝난 후 전체 결과를 알기 위한 메서드
   */
  private void recordCollectResult(ExecutionContext context, ArticleCollectStepResultDto result) {
    increment(context, COLLECTED_COUNT, result.collectedCount());
    increment(context, DUPLICATE_SKIPPED_COUNT, result.duplicateSkippedCount());

    if (result.source() == ArticleSource.NAVER) {
      increment(context, NAVER_REQUEST_COUNT, result.requestCount());
      increment(context, NAVER_SUCCESS_COUNT, result.successCount());
      increment(context, NAVER_FAILURE_COUNT, result.failureCount());
      return;
    }

    increment(context, RSS_REQUEST_COUNT, result.requestCount());
    increment(context, RSS_SUCCESS_COUNT, result.successCount());
    increment(context, RSS_FAILURE_COUNT, result.failureCount());

    if (result.source() == ArticleSource.YEONHAP) {
      increment(context, YEONHAP_RSS_REQUEST_COUNT, result.requestCount());
      increment(context, YEONHAP_RSS_SUCCESS_COUNT, result.successCount());
      increment(context, YEONHAP_RSS_FAILURE_COUNT, result.failureCount());
    }
  }

  /**
   * ExecutionContext에 저장된 정수 값을 누적합니다.
   */
  private void increment(ExecutionContext context, String key, int amount) {
    context.putInt(key, context.getInt(key, 0) + amount);
  }

  /**
   * Job ExecutionContext에 모인 중간 카운트와 저장/연결 결과를 합쳐 최종 결과 DTO를 만듭니다.
   */
  private ArticleCollectResultDto toFinalResult(ExecutionContext context,
      int articleInterestLinkedCount) {
    int naverFailureCount = context.getInt(NAVER_FAILURE_COUNT, 0);
    int rssFailureCount = context.getInt(RSS_FAILURE_COUNT, 0);

    return new ArticleCollectResultDto(
        context.getInt(NAVER_REQUEST_COUNT, 0),
        context.getInt(NAVER_SUCCESS_COUNT, 0),
        naverFailureCount,
        false,
        context.getInt(RSS_REQUEST_COUNT, 0),
        context.getInt(RSS_SUCCESS_COUNT, 0),
        rssFailureCount,
        context.getInt(YEONHAP_RSS_REQUEST_COUNT, 0),
        context.getInt(YEONHAP_RSS_SUCCESS_COUNT, 0),
        context.getInt(YEONHAP_RSS_FAILURE_COUNT, 0),
        context.getInt(COLLECTED_COUNT, 0),
        context.getInt(SAVED_COUNT, 0),
        context.getInt(DUPLICATE_SKIPPED_COUNT, 0),
        context.getInt(INVALID_SKIPPED_COUNT, 0),
        articleInterestLinkedCount,
        naverFailureCount + rssFailureCount
    );
  }
}
