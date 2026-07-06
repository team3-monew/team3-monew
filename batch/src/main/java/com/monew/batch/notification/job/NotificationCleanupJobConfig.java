package com.monew.batch.notification.job;

import com.monew.batch.monitoring.BatchJobMetricsListener;
import com.monew.batch.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationCleanupJobConfig {

    private final NotificationRepository notificationRepository;

    @Bean
    public Job notificationCleanupJob(
            JobRepository jobRepository,
            Step notificationCleanupStep,
            BatchJobMetricsListener batchJobMetricsListener
    ) {
        return new JobBuilder("notificationCleanupJob", jobRepository)
                .listener(batchJobMetricsListener)
                .start(notificationCleanupStep)
                .build();
    }

    @Bean
    public Step notificationCleanupStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("notificationCleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime threshold = LocalDateTime.now().minusDays(7);

                    int deletedCount = notificationRepository.deleteConfirmedBefore(threshold);

                    contribution.incrementWriteCount(deletedCount);

                    log.info("확인 후 7일 지난 알림 삭제 완료 - threshold={}, deletedCount={}",
                            threshold, deletedCount);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}