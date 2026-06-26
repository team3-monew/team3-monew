package com.monew.batch.article.collect.config;

import com.monew.batch.article.collect.dto.ArticleCollectResultDto;
import com.monew.batch.article.collect.service.ArticleCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 기사 수집 작업을 Spring Batch Job/Step으로 등록하는 설정 클래스입니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArticleCollectJobConfig {

  private final ArticleCollectService articleCollectService;

  /**
   * 매 시간 실행될 기사 수집 Batch Job입니다.
   */
  @Bean
  public Job articleCollectJob(JobRepository jobRepository, Step articleCollectStep) {
    return new JobBuilder("articleCollectJob", jobRepository)
        .start(articleCollectStep)
        .build();
  }

  /**
   * 기사 수집을 한 번 수행하는 Step입니다.
   */
  @Bean
  public Step articleCollectStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager) {

    return new StepBuilder("articleCollectStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          ArticleCollectResultDto result = articleCollectService.collect();
          log.info("기사 수집 step 끝. result={}", result);
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();

  }
}
