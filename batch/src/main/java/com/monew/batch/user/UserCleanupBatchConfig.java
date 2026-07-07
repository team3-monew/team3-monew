package com.monew.batch.user;

import com.monew.batch.monitoring.BatchJobMetricsListener;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 사용자 물리삭제 배치.
 * 논리삭제(deleted_at) 후 일정 기간(기본 1일) 경과한 사용자를 물리 삭제.
 * 실제 삭제 로직(PostgreSQL + Mongo 활동내역)은 {@link UserCleaner} 참고.
 */
@Configuration
@RequiredArgsConstructor
public class UserCleanupBatchConfig {

    public static final String JOB_NAME = "userCleanupJob";

    private final UserCleaner userCleaner;

    @Value("${monew.batch.user-retention-days:1}")
    private long retentionDays;

    @Bean
    public Job userCleanupJob(JobRepository jobRepository, Step userCleanupStep,
        BatchJobMetricsListener batchJobMetricsListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                // Job 종료 시 공통 실행/성공/실패/소요시간 메트릭을 기록합니다.
                .listener(batchJobMetricsListener)
                .start(userCleanupStep)
                .build();
    }

    @Bean
    public Step userCleanupStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("userCleanupStep", jobRepository)
                .tasklet(userCleanupTasklet(), txManager)
                .build();
    }

    @Bean
    public Tasklet userCleanupTasklet() {
        return (contribution, chunkContext) -> {
            userCleaner.cleanup(LocalDateTime.now().minusDays(retentionDays));
            return RepeatStatus.FINISHED;
        };
    }
}
