package com.monew.batch.article.scheduler;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCollectScheduler {

  private final JobLauncher jobLauncher;
  @Qualifier("articleCollectJob")
  private final Job articleCollectJob;

  /**
   * 매 시간 기사 수집 Spring Batch Job을 실행.
   */
  @Scheduled(cron = "${monew.article-collect.cron}")
  public void collectHourly() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLocalDateTime("runAt", LocalDateTime.now())
        .toJobParameters();

    log.info("기사 수집 Job 실행 시작. parameters={}", jobParameters);
    jobLauncher.run(articleCollectJob, jobParameters);
  }
}
