package com.monew.batch.notification.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monew.batch.monitoring.BatchJobMetricsListener;
import com.monew.batch.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

class NotificationCleanupJobConfigTest {

    @Test
    @DisplayName("알림 정리 Job 구성 성공 - Job과 Step 이름을 정확히 등록한다")
    void createNotificationCleanupJobAndStep_success() {
        // Given
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        JobRepository jobRepository = mock(JobRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        BatchJobMetricsListener batchJobMetricsListener = mock(BatchJobMetricsListener.class);

        NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(notificationRepository);

        // When
        Step step = config.notificationCleanupStep(jobRepository, transactionManager);
        Job job = config.notificationCleanupJob(jobRepository, step, batchJobMetricsListener);

        // Then
        assertThat(step.getName()).isEqualTo("notificationCleanupStep");
        assertThat(job.getName()).isEqualTo("notificationCleanupJob");
    }

    @Test
    @DisplayName("알림 정리 성공 - 삭제 대상 알림을 chunk 단위로 조회하고 삭제한다")
    void cleanupConfirmedNotifications_success_chunkDelete() {
        // Given
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(notificationRepository);

        LocalDateTime threshold = LocalDateTime.of(2026, 7, 6, 0, 0);
        PageRequest pageRequest = PageRequest.of(0, NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE);

        List<UUID> firstChunk = IntStream.range(0, NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        List<UUID> secondChunk = List.of(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(notificationRepository.findConfirmedIdsBefore(threshold, pageRequest))
                .thenReturn(firstChunk)
                .thenReturn(secondChunk);

        when(notificationRepository.deleteAllByIdInBulk(firstChunk))
                .thenReturn(firstChunk.size());

        when(notificationRepository.deleteAllByIdInBulk(secondChunk))
                .thenReturn(secondChunk.size());

        // When
        int deletedCount = config.cleanupConfirmedNotifications(threshold);

        // Then
        assertThat(deletedCount).isEqualTo(NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE + secondChunk.size());

        verify(notificationRepository).deleteAllByIdInBulk(firstChunk);
        verify(notificationRepository).deleteAllByIdInBulk(secondChunk);
    }

    @Test
    @DisplayName("알림 정리 성공 - 삭제 대상이 없으면 삭제 쿼리를 실행하지 않는다")
    void cleanupConfirmedNotifications_success_empty() {
        // Given
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(notificationRepository);

        LocalDateTime threshold = LocalDateTime.of(2026, 7, 6, 0, 0);
        PageRequest pageRequest = PageRequest.of(0, NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE);

        when(notificationRepository.findConfirmedIdsBefore(threshold, pageRequest))
                .thenReturn(List.of());

        // When
        int deletedCount = config.cleanupConfirmedNotifications(threshold);

        // Then
        assertThat(deletedCount).isZero();

        verify(notificationRepository, never()).deleteAllByIdInBulk(anyList());
    }

    @Test
    @DisplayName("알림 정리 성공 - 삭제 대상 조회 결과가 chunk size보다 작으면 추가 조회 없이 종료한다")
    void cleanupConfirmedNotifications_success_stopWhenLastChunk() {
        // Given
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(notificationRepository);

        LocalDateTime threshold = LocalDateTime.of(2026, 7, 6, 0, 0);
        PageRequest pageRequest = PageRequest.of(0, NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE);

        List<UUID> lastChunk = List.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(notificationRepository.findConfirmedIdsBefore(threshold, pageRequest))
                .thenReturn(lastChunk);

        when(notificationRepository.deleteAllByIdInBulk(lastChunk))
                .thenReturn(lastChunk.size());

        // When
        int deletedCount = config.cleanupConfirmedNotifications(threshold);

        // Then
        assertThat(deletedCount).isEqualTo(lastChunk.size());

        verify(notificationRepository).deleteAllByIdInBulk(lastChunk);
    }

    @Test
    @DisplayName("알림 정리 성공 - 삭제 쿼리 결과가 0이면 무한 반복을 방지하고 종료한다")
    void cleanupConfirmedNotifications_success_stopWhenDeleteCountZero() {
        // Given
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(notificationRepository);

        LocalDateTime threshold = LocalDateTime.of(2026, 7, 6, 0, 0);
        PageRequest pageRequest = PageRequest.of(0, NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE);

        List<UUID> chunk = IntStream.range(0, NotificationCleanupJobConfig.CLEANUP_CHUNK_SIZE)
                .mapToObj(i -> UUID.randomUUID())
                .toList();

        when(notificationRepository.findConfirmedIdsBefore(threshold, pageRequest))
                .thenReturn(chunk);

        when(notificationRepository.deleteAllByIdInBulk(chunk))
                .thenReturn(0);

        // When
        int deletedCount = config.cleanupConfirmedNotifications(threshold);

        // Then
        assertThat(deletedCount).isZero();

        verify(notificationRepository).deleteAllByIdInBulk(chunk);
    }
}