package com.monew.batch.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 개발용 스케줄 트리거.
 * 잡 로직(UserCleanupBatchConfig)과 분리되어 있어, 운영에서 EventBridge → RunTask 로 전환 시
 * 'scheduler' 프로파일만 제외 (잡 코드 재사용)
 */
@Slf4j
@Component
@Profile("scheduler")
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final JobLauncher jobLauncher;
    private final Job userCleanupJob;

    @Scheduled(cron = "${monew.batch.user-cleanup-cron:0 0 4 * * *}") // 매일 04:00
    public void run() throws Exception {
        log.info("[userCleanup] 스케줄 트리거 실행");
        jobLauncher.run(userCleanupJob, new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis()) // 재실행을 위한 유니크 파라미터
                .toJobParameters());
    }
}
