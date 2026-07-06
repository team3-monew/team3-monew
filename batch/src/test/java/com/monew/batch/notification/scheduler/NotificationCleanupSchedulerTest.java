package com.monew.batch.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job notificationCleanupJob;

    @Test
    @DisplayName("알림 정리 스케줄러 실행 성공 - timestamp 파라미터로 정리 Job을 실행한다")
    void runNotificationCleanupJob_success() throws Exception {
        // Given
        NotificationCleanupScheduler scheduler = new NotificationCleanupScheduler(jobLauncher, notificationCleanupJob);
        JobExecution jobExecution = new JobExecution(1L);
        jobExecution.setStatus(BatchStatus.COMPLETED);
        jobExecution.setExitStatus(ExitStatus.COMPLETED);

        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(jobExecution);

        // When
        scheduler.runNotificationCleanupJob();

        // Then
        ArgumentCaptor<JobParameters> parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(notificationCleanupJob), parametersCaptor.capture());

        JobParameter<?> timestamp = parametersCaptor.getValue().getParameter("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getValue()).isInstanceOf(Long.class);
    }

    @Test
    @DisplayName("알림 정리 스케줄러 실행 실패 - Job 상태가 COMPLETED가 아니면 실패 로그 분기로 진입한다")
    void runNotificationCleanupJob_failedStatus() throws Exception {
        // Given
        NotificationCleanupScheduler scheduler = new NotificationCleanupScheduler(jobLauncher, notificationCleanupJob);
        JobExecution jobExecution = new JobExecution(1L);
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.setExitStatus(ExitStatus.FAILED);

        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(jobExecution);

        // When
        scheduler.runNotificationCleanupJob();

        // Then
        verify(jobLauncher).run(eq(notificationCleanupJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("알림 정리 스케줄러 예외 처리 - Job 실행 실패가 발생해도 예외를 밖으로 던지지 않는다")
    void runNotificationCleanupJob_catchesException() throws Exception {
        // Given
        NotificationCleanupScheduler scheduler = new NotificationCleanupScheduler(jobLauncher, notificationCleanupJob);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenThrow(new RuntimeException("batch failed"));

        // When
        scheduler.runNotificationCleanupJob();

        // Then
        verify(jobLauncher).run(eq(notificationCleanupJob), any(JobParameters.class));
    }
}