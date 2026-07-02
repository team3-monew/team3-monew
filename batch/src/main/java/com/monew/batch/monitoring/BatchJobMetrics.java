package com.monew.batch.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobMetrics {

  private static final String JOB_TAG = "job";

  private final MeterRegistry meterRegistry;  // 메트릭 등록 및 값 기록에 사용하는 Micrometer 객체
  private final Map<String, AtomicLong> lastSuccessTimes = new ConcurrentHashMap<>();

  /**
   * Job이 성공으로 끝났을 때 공통 메트릭을 갱신합니다.
   * Counter/Timer는 누적되고, 마지막 성공 시각 Gauge는 최신 값으로 덮어씁니다.
   */
  public void recordSuccess(String jobName, Duration duration) {
    String normalizedJobName = BatchJobNames.normalize(jobName);
    recordRun(normalizedJobName, duration);

    meterRegistry.counter("monew.batch.job.success.total", JOB_TAG, normalizedJobName).increment();
    lastSuccessTime(normalizedJobName).set(Instant.now().getEpochSecond());
  }

  /**
   * Job이 실패로 끝났을 때 공통 메트릭을 갱신합니다.
   * 실패도 전체 실행 횟수와 실행 시간에는 포함됩니다.
   */
  public void recordFailure(String jobName, Duration duration) {
    String normalizedJobName = BatchJobNames.normalize(jobName);
    recordRun(normalizedJobName, duration);

    meterRegistry.counter("monew.batch.job.failure.total", JOB_TAG, normalizedJobName).increment();
  }

  // 모든 Job에 공통으로 필요한 실행 횟수와 실행 시간을 기록합니다.
  private void recordRun(String jobName, Duration duration) {
    // 배치 Job 실행 횟수 1 증가
    meterRegistry.counter("monew.batch.job.run.total", JOB_TAG, jobName).increment();
    Timer.builder("monew.batch.job.duration")
        .tag(JOB_TAG, jobName)
        .register(meterRegistry)
        .record(Math.max(duration.toMillis(), 0L), TimeUnit.MILLISECONDS);
  }

  // Gauge는 참조 객체가 유지되어야 값이 노출되므로 AtomicLong을 Map에 보관합니다.
  private AtomicLong lastSuccessTime(String jobName) {
    return lastSuccessTimes.computeIfAbsent(jobName, key -> {
      AtomicLong value = new AtomicLong(0L);
      meterRegistry.gauge("monew.batch.job.last_success_time", Tags.job(key), value);
      return value;
    });
  }

  private static final class Tags {

    private Tags() {
    }

    private static Iterable<Tag> job(String jobName) {
      return List.of(Tag.of(JOB_TAG, jobName));
    }
  }
}
