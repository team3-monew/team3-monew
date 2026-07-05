package com.monew.batch.article.backup.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import com.monew.batch.article.backup.dto.ArticleBackupResultDto;
import com.monew.batch.article.backup.service.ArticleBackupService;
import com.monew.batch.monitoring.ArticleBackupMetrics;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

@ExtendWith(MockitoExtension.class)
class ArticleBackupTaskletTest {

  private static final LocalDate TARGET_DATE = LocalDate.of(2026, 6, 29);

  @Mock
  private ArticleBackupService articleBackupService;

  @Mock
  private ArticleBackupMetrics articleBackupMetrics;

  @Test
  @DisplayName("기사 백업 Tasklet 실행 성공 - targetDate 파라미터를 LocalDate로 파싱하고 결과 메트릭을 기록한다")
  void execute_success() {
    // given
    ArticleBackupTasklet tasklet = new ArticleBackupTasklet(articleBackupService,
        articleBackupMetrics);
    ArticleBackupResultDto result = new ArticleBackupResultDto(2L, 2L, 0L, 1L, 128L);
    given(articleBackupService.backup(TARGET_DATE)).willReturn(result);

    // when
    RepeatStatus repeatStatus = tasklet.execute(
        stepContribution("2026-06-29"),
        chunkContext("2026-06-29")
    );

    // then
    assertThat(repeatStatus).isEqualTo(RepeatStatus.FINISHED);
    then(articleBackupService).should().backup(TARGET_DATE);
    then(articleBackupMetrics).should().recordResult(result);
    then(articleBackupMetrics).should(never()).recordFailure();
  }

  @Test
  @DisplayName("기사 백업 Tasklet 실행 실패 - 서비스 예외가 발생하면 실패 메트릭을 기록하고 예외를 전파한다")
  void execute_recordsFailureWhenServiceThrowsException() {
    // given
    ArticleBackupTasklet tasklet = new ArticleBackupTasklet(articleBackupService,
        articleBackupMetrics);
    RuntimeException exception = new RuntimeException("backup failed");
    given(articleBackupService.backup(TARGET_DATE)).willThrow(exception);

    // when & then
    assertThatThrownBy(() -> tasklet.execute(
        stepContribution("2026-06-29"),
        chunkContext("2026-06-29")
    )).isSameAs(exception);

    then(articleBackupService).should().backup(TARGET_DATE);
    then(articleBackupMetrics).should().recordFailure();
    then(articleBackupMetrics).should(never()).recordResult(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("기사 백업 Tasklet 실행 실패 - targetDate 파라미터가 없으면 NullPointerException이 발생한다")
  void execute_throwsExceptionWhenTargetDateParameterMissing() {
    // given
    ArticleBackupTasklet tasklet = new ArticleBackupTasklet(articleBackupService,
        articleBackupMetrics);

    // when & then
    assertThatThrownBy(() -> tasklet.execute(
        stepContributionWithoutTargetDate(),
        chunkContextWithoutTargetDate()
    )).isInstanceOf(NullPointerException.class);

    verifyNoInteractions(articleBackupService);
    verifyNoInteractions(articleBackupMetrics);
  }

  @Test
  @DisplayName("기사 백업 Tasklet 실행 실패 - targetDate 형식이 잘못되면 DateTimeParseException이 발생한다")
  void execute_throwsExceptionWhenTargetDateFormatInvalid() {
    // given
    ArticleBackupTasklet tasklet = new ArticleBackupTasklet(articleBackupService,
        articleBackupMetrics);

    // when & then
    assertThatThrownBy(() -> tasklet.execute(
        stepContribution("invalid-date"),
        chunkContext("invalid-date")
    )).isInstanceOf(DateTimeParseException.class);

    verifyNoInteractions(articleBackupService);
    verifyNoInteractions(articleBackupMetrics);
  }

  private StepContribution stepContribution(String targetDate) {
    return new StepContribution(stepExecution(jobParameters(targetDate)));
  }

  private ChunkContext chunkContext(String targetDate) {
    return new ChunkContext(new StepContext(stepExecution(jobParameters(targetDate))));
  }

  private StepContribution stepContributionWithoutTargetDate() {
    return new StepContribution(stepExecution(new JobParameters()));
  }

  private ChunkContext chunkContextWithoutTargetDate() {
    return new ChunkContext(new StepContext(stepExecution(new JobParameters())));
  }

  private StepExecution stepExecution(JobParameters jobParameters) {
    return MetaDataInstanceFactory.createStepExecution(jobParameters);
  }

  private JobParameters jobParameters(String targetDate) {
    return new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .toJobParameters();
  }
}
