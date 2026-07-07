package com.monew.batch.article.backup.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.monew.batch.article.backup.tasklet.ArticleBackupTasklet;
import com.monew.batch.monitoring.BatchJobMetricsListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class ArticleBackupJobConfigTest {

  @Test
  @DisplayName("기사 백업 Job 구성 성공 - Job 이름과 Step 이름을 정확히 등록한다")
  void createArticleBackupJobAndStep_success() {
    // given
    ArticleBackupTasklet articleBackupTasklet = mock(ArticleBackupTasklet.class);
    JobRepository jobRepository = mock(JobRepository.class);
    PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    BatchJobMetricsListener batchJobMetricsListener = mock(BatchJobMetricsListener.class);
    ArticleBackupJobConfig config = new ArticleBackupJobConfig(articleBackupTasklet);

    // when
    Step step = config.articleBackupStep(jobRepository, transactionManager);
    Job job = config.articleBackupJob(jobRepository, step, batchJobMetricsListener);

    // then
    assertThat(step.getName()).isEqualTo("articleBackupStep");
    assertThat(job.getName()).isEqualTo("articleBackupJob");
  }
}
