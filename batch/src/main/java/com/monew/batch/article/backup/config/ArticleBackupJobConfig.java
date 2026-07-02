package com.monew.batch.article.backup.config;

import com.monew.batch.article.backup.tasklet.ArticleBackupTasklet;
import com.monew.batch.monitoring.BatchJobMetricsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 기사 백업용 Spring Batch Job 과 Step 을 등록합니다.
 */
@Configuration
@RequiredArgsConstructor
public class ArticleBackupJobConfig {

  private final ArticleBackupTasklet articleBackupTasklet;

  @Bean
  public Job articleBackupJob(JobRepository jobRepository, Step articleBackupStep,
      BatchJobMetricsListener batchJobMetricsListener) {
    return new JobBuilder("articleBackupJob", jobRepository)
        .listener(batchJobMetricsListener)  // Job 종료 시 공통 실행/성공/실패/소요시간 메트릭을 기록합니다.
        .start(articleBackupStep)
        .build();
  }

  @Bean
  public Step articleBackupStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {
    return new StepBuilder("articleBackupStep", jobRepository)
        .tasklet(articleBackupTasklet, transactionManager)
        .build();
  }
}
