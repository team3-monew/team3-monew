package com.monew.batch.monitoring;

import com.monew.batch.article.backup.dto.ArticleBackupResultDto;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleBackupMetrics {

  private final MeterRegistry meterRegistry;
  private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

  /**
   * 기사 백업 Job의 마지막 실행 결과를 Gauge로 기록합니다.
   * failure.count는 실패한 기사 수를 정확히 알 수 있는 경우에만 결과 DTO 값으로 기록합니다.
   */
  public void recordResult(ArticleBackupResultDto result) {
    set("monew.batch.article.backup.last.target.count", result.targetCount());
    set("monew.batch.article.backup.last.success.count", result.successCount());
    set("monew.batch.article.backup.last.failure.count", result.failureCount());
    set("monew.batch.article.backup.last.file.count", result.fileCount());
    set("monew.batch.article.backup.last.file.size.bytes", result.fileSizeBytes());
    set("monew.batch.article.backup.last.failed", 0L);
  }

  /**
   * 백업 실행 자체가 실패했음을 표시합니다.
   * 예외 시점에는 실패 기사 수를 모르기 때문에 failure.count는 갱신하지 않습니다.
   * 1L = 실패 / 0L = 성공
   */
  public void recordFailure() {
    set("monew.batch.article.backup.last.failed", 1L);
  }

  private void set(String metricName, long value) {
    gauges.computeIfAbsent(metricName, key -> {
      AtomicLong gaugeValue = new AtomicLong();
      meterRegistry.gauge(key, gaugeValue);
      return gaugeValue;
    }).set(value);
  }
}
