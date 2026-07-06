package com.monew.batch.article.collect.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.monew.batch.article.collect.dto.ArticleCollectStagingCleanupResultDto;
import com.monew.batch.article.collect.dto.ArticleCollectStepResultDto;
import com.monew.batch.article.collect.dto.ArticleSaveAndInterestLinkStepResultDto;
import com.monew.batch.article.collect.dto.ArticleSourceCollectResultDto;
import com.monew.batch.article.collect.service.ArticleCollectService;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.entity.ArticleSource;
import com.monew.batch.monitoring.ArticleCollectMetrics;
import com.monew.batch.monitoring.BatchJobMetricsListener;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class ArticleCollectJobConfigTest {

  private static final long JOB_INSTANCE_ID = 1L;
  private static final ExitStatus COMPLETED_WITH_EXTERNAL_API_FAILURE =
      new ExitStatus("COMPLETED_WITH_EXTERNAL_API_FAILURE");

  @Mock
  ArticleCollectService articleCollectService;

  @Mock
  ArticleCollectMetrics articleCollectMetrics;

  @Mock
  JobRepository jobRepository;

  PlatformTransactionManager transactionManager;
  ArticleCollectJobConfig config;

  @BeforeEach
  void setUp() {
    transactionManager = new ResourcelessTransactionManager();
    config = new ArticleCollectJobConfig(
        articleCollectService,
        articleCollectMetrics,
        new ArticleCollectProperties(100, 100, 1, 0, 0, "0 0 * * * *")
    );
  }

  @Test
  @DisplayName("성공 - Naver 수집 step이 실패해도 다음 RSS step으로 넘어갈 수 있는 exit status를 남긴다")
  void naverCollectStep_success_returnsExternalApiFailureExitStatus_whenServiceThrowsException()
      throws Exception {
    // given
    given(articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID))
        .willThrow(new RuntimeException("naver api failed"));
    Step step = config.naverCollectStep(jobRepository, transactionManager);
    StepExecution stepExecution = stepExecution("naverCollectStep");

    // when
    step.execute(stepExecution);

    // then
    assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(stepExecution.getExitStatus()).isEqualTo(COMPLETED_WITH_EXTERNAL_API_FAILURE);
  }

  @Test
  @DisplayName("성공 - RSS 일부 실패 시 COMPLETED_WITH_EXTERNAL_API_FAILURE exit status가 잡힌다")
  void rssCollectStep_success_setsExternalApiFailureExitStatus_whenFailureCountExists()
      throws Exception {
    // given
    given(articleCollectService.collectRssArticlesToStaging(JOB_INSTANCE_ID, ArticleSource.HANKYUNG))
        .willReturn(new ArticleCollectStepResultDto(
            ArticleSource.HANKYUNG,
            2,
            1,
            1,
            5,
            4,
            3,
            1
        ));
    Step step = config.hankyungRssCollectStep(jobRepository, transactionManager);
    StepExecution stepExecution = stepExecution("hankyungRssCollectStep");

    // when
    step.execute(stepExecution);

    // then
    assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(stepExecution.getExitStatus()).isEqualTo(COMPLETED_WITH_EXTERNAL_API_FAILURE);
  }

  @Test
  @DisplayName("성공 - collect step 결과가 ExecutionContext에 누적된다")
  void collectStep_success_accumulatesResultIntoExecutionContext() throws Exception {
    // given
    given(articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID))
        .willReturn(new ArticleCollectStepResultDto(
            ArticleSource.NAVER,
            3,
            2,
            1,
            10,
            8,
            6,
            2
        ));
    given(articleCollectService.collectRssArticlesToStaging(JOB_INSTANCE_ID, ArticleSource.HANKYUNG))
        .willReturn(new ArticleCollectStepResultDto(
            ArticleSource.HANKYUNG,
            2,
            2,
            0,
            7,
            5,
            4,
            1
        ));
    JobExecution jobExecution = jobExecution();
    Step naverStep = config.naverCollectStep(jobRepository, transactionManager);
    Step hankyungStep = config.hankyungRssCollectStep(jobRepository, transactionManager);

    // when
    naverStep.execute(stepExecution("naverCollectStep", jobExecution));
    hankyungStep.execute(stepExecution("hankyungRssCollectStep", jobExecution));

    // then
    ExecutionContext context = jobExecution.getExecutionContext();
    assertThat(context.getInt("naverRequestCount")).isEqualTo(3);
    assertThat(context.getInt("naverSuccessCount")).isEqualTo(2);
    assertThat(context.getInt("naverFailureCount")).isEqualTo(1);
    assertThat(context.getInt("rssRequestCount")).isEqualTo(2);
    assertThat(context.getInt("rssSuccessCount")).isEqualTo(2);
    assertThat(context.getInt("rssFailureCount")).isZero();
    assertThat(context.getInt("collectedCount")).isEqualTo(17);
    assertThat(context.getInt("duplicateSkippedCount")).isEqualTo(3);
  }

  @Test
  @DisplayName("성공 - articlePersistStep이 staging 저장 결과를 metrics에 기록한다")
  void articlePersistStep_success_recordsPersistResultToMetrics() throws Exception {
    // given
    ArticleSaveAndInterestLinkStepResultDto result = persistResult();
    given(articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID))
        .willReturn(result);
    Step step = config.articlePersistStep(jobRepository, transactionManager);
    StepExecution stepExecution = stepExecution("articlePersistStep");

    // when
    step.execute(stepExecution);

    // then
    then(articleCollectMetrics).should().recordPersistResult(result);
    assertThat(stepExecution.getJobExecution().getExecutionContext().getInt("savedCount"))
        .isEqualTo(3);
    assertThat(stepExecution.getJobExecution().getExecutionContext().getInt("invalidSkippedCount"))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("성공 - persist 성공 후 cleanup step이 실행된다")
  void articleCollectJob_success_executesCleanupStep_afterPersistSuccess() throws Exception {
    // given
    given(articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID))
        .willReturn(ArticleCollectStepResultDto.empty(ArticleSource.NAVER));
    given(articleCollectService.collectRssArticlesToStaging(JOB_INSTANCE_ID, ArticleSource.HANKYUNG))
        .willReturn(ArticleCollectStepResultDto.empty(ArticleSource.HANKYUNG));
    given(articleCollectService.collectRssArticlesToStaging(JOB_INSTANCE_ID, ArticleSource.CHOSUN))
        .willReturn(ArticleCollectStepResultDto.empty(ArticleSource.CHOSUN));
    given(articleCollectService.collectRssArticlesToStaging(JOB_INSTANCE_ID, ArticleSource.YEONHAP))
        .willReturn(ArticleCollectStepResultDto.empty(ArticleSource.YEONHAP));
    given(articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID))
        .willReturn(persistResult());
    given(articleCollectService.cleanupStaging(JOB_INSTANCE_ID))
        .willReturn(new ArticleCollectStagingCleanupResultDto(5L));

    Step naverStep = config.naverCollectStep(jobRepository, transactionManager);
    Step hankyungStep = config.hankyungRssCollectStep(jobRepository, transactionManager);
    Step chosunStep = config.chosunRssCollectStep(jobRepository, transactionManager);
    Step yeonhapStep = config.yeonhapRssCollectStep(jobRepository, transactionManager);
    Step persistStep = config.articlePersistStep(jobRepository, transactionManager);
    Step cleanupStep = config.articleCollectStagingCleanupStep(jobRepository, transactionManager);
    Job job = config.articleCollectJob(
        jobRepository,
        naverStep,
        hankyungStep,
        chosunStep,
        yeonhapStep,
        persistStep,
        cleanupStep,
        mock(BatchJobMetricsListener.class)
    );
    JobExecution jobExecution = jobExecution();

    // when
    job.execute(jobExecution);

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    then(articleCollectService).should().saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);
    then(articleCollectService).should().cleanupStaging(JOB_INSTANCE_ID);
  }

  private ArticleSaveAndInterestLinkStepResultDto persistResult() {
    return new ArticleSaveAndInterestLinkStepResultDto(
        6,
        3,
        2,
        1,
        4,
        List.of(new ArticleSourceCollectResultDto("NAVER", 6, 3, 2, 1))
    );
  }

  private StepExecution stepExecution(String stepName) {
    return stepExecution(stepName, jobExecution());
  }

  private StepExecution stepExecution(String stepName, JobExecution jobExecution) {
    return new StepExecution(stepName, jobExecution);
  }

  private JobExecution jobExecution() {
    JobInstance jobInstance = new JobInstance(JOB_INSTANCE_ID, "articleCollectJob");
    return new JobExecution(jobInstance, new JobParameters());
  }
}
