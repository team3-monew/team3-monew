package com.monew.batch.notification.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationCleanupScheduler {

    private final JobLauncher jobLauncher;
    private final Job notificationCleanupJob;

    public NotificationCleanupScheduler(
            JobLauncher jobLauncher,
            @Qualifier("notificationCleanupJob") Job notificationCleanupJob
    ) {
        this.jobLauncher = jobLauncher;
        this.notificationCleanupJob = notificationCleanupJob;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runNotificationCleanupJob() {
        try {
            jobLauncher.run(
                    notificationCleanupJob,
                    new JobParametersBuilder()
                            .addLong("timestamp", System.currentTimeMillis())
                            .toJobParameters()
            );

            log.info("알림 정리 배치 실행 완료");
        } catch (Exception e) {
            log.error("알림 정리 배치 실행 실패", e);
        }
    }
}