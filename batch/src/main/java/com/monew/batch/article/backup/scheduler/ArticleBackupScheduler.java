package com.monew.batch.article.backup.scheduler;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 정해진 시각에 기사 백업 Job 을 실행하는 스케줄러입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "backup.enabled", havingValue = "true", matchIfMissing = true)
public class ArticleBackupScheduler {

  private final JobLauncher jobLauncher;

  @Qualifier("articleBackupJob")
  private final Job articleBackupJob;

  @Scheduled(cron = "${backup.cron:0 30 3 * * *}")
  public void backupDaily() throws Exception {
    LocalDate targetDate = LocalDate.now().minusDays(1);
    JobParameters jobParameters = new JobParametersBuilder()
        .addString("targetDate", targetDate.toString())
        .addLong("timestamp", System.currentTimeMillis())
        .toJobParameters();

    log.info("[article backup] 기사 백업 job 시작. 백업 날짜={}, parameters={}", targetDate, jobParameters);
    jobLauncher.run(articleBackupJob, jobParameters);
  }
}