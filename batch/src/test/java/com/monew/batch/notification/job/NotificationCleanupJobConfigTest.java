package com.monew.batch.notification.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.monew.batch.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class NotificationCleanupJobConfigTest {

    @Test
    @DisplayName("알림 정리 Job 구성 성공 - Job과 Step 이름을 정확히 등록한다")
    void createNotificationCleanupJobAndStep_success() {
        // Given
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(notificationRepository);

        // When
        Step step = config.notificationCleanupStep(jobRepository, transactionManager);
        Job job = config.notificationCleanupJob(jobRepository, step);

        // Then
        assertThat(step.getName()).isEqualTo("notificationCleanupStep");
        assertThat(job.getName()).isEqualTo("notificationCleanupJob");
    }
}
