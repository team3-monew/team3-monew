package com.monew.batch.user;

import com.monew.batch.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 논리삭제(deleted_at) 후 일정 기간(기본 1일) 경과한 사용자를 물리 삭제
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserCleanupBatchConfig {

    public static final String JOB_NAME = "userCleanupJob";

    private final UserRepository userRepository;

    @Value("${monew.batch.user-retention-days:1}")
    private long retentionDays;

    @Bean
    public Job userCleanupJob(JobRepository jobRepository, Step userCleanupStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
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
            LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
            int deleted = userRepository.deleteAllByDeletedAtBefore(threshold);
            log.info("[userCleanup] 물리 삭제 사용자 수={}, 기준={} 이전", deleted, threshold);
            return RepeatStatus.FINISHED;
        };
    }
}
