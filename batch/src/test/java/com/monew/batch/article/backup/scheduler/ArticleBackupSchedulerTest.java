package com.monew.batch.article.backup.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class ArticleBackupSchedulerTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job articleBackupJob;

  @Test
  @DisplayName("기사 백업 스케줄러 실행 성공 - 전날 날짜와 timestamp 파라미터로 백업 Job을 실행한다")
  void backupDaily_success() throws Exception {
    // given
    ArticleBackupScheduler scheduler = new ArticleBackupScheduler(jobLauncher, articleBackupJob);
    LocalDate expectedTargetDate = LocalDate.now().minusDays(1);
    given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .willReturn(mock(JobExecution.class));

    // when
    scheduler.backupDaily();

    // then
    ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
    then(jobLauncher).should().run(eq(articleBackupJob), parametersCaptor.capture());

    JobParameters jobParameters = parametersCaptor.getValue();
    JobParameter<?> targetDate = jobParameters.getParameter("targetDate");
    JobParameter<?> timestamp = jobParameters.getParameter("timestamp");

    assertThat(targetDate).isNotNull();
    assertThat(targetDate.getValue()).isEqualTo(expectedTargetDate.toString());
    assertThat(timestamp).isNotNull();
    assertThat(timestamp.getValue()).isInstanceOf(Long.class);
  }

  @Test
  @DisplayName("기사 백업 스케줄러 실행 실패 - JobLauncher 예외가 발생하면 예외를 밖으로 전파한다")
  void backupDaily_throwsExceptionWhenJobLauncherFails() throws Exception {
    // given
    ArticleBackupScheduler scheduler = new ArticleBackupScheduler(jobLauncher, articleBackupJob);
    RuntimeException exception = new RuntimeException("batch failed");
    given(jobLauncher.run(any(Job.class), any(JobParameters.class))).willThrow(exception);

    // when & then
    assertThatThrownBy(scheduler::backupDaily)
        .isSameAs(exception);
    then(jobLauncher).should().run(eq(articleBackupJob), any(JobParameters.class));
  }
}
