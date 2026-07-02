package com.monew.batch.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch Job 종료 시 실행 결과를 메트릭으로 기록하는 공통 리스너입니다.
 * Job 이름, 성공/실패 여부, 실행 시간을 BatchJobMetrics에 전달합니다.
 */
@Component
@RequiredArgsConstructor
public class BatchJobMetricsListener implements JobExecutionListener {

  private final BatchJobMetrics batchJobMetrics;

  /**
   * Spring Batch가 Job 종료 후 호출하는 hook입니다.
   * 각 JobConfig에 listener로 연결하면 성공/실패/소요시간이 공통 메트릭으로 기록됩니다.
   */
  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    Duration duration = duration(jobExecution);

    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      batchJobMetrics.recordSuccess(jobName, duration);
      return;
    }

    batchJobMetrics.recordFailure(jobName, duration);
  }

  private Duration duration(JobExecution jobExecution) {
    LocalDateTime startTime = jobExecution.getStartTime();
    LocalDateTime endTime = jobExecution.getEndTime();
    if (startTime == null || endTime == null) {
      return Duration.ZERO;
    }

    ZoneId zoneId = ZoneId.systemDefault();
    return Duration.between(startTime.atZone(zoneId).toInstant(), endTime.atZone(zoneId).toInstant());
  }
}
