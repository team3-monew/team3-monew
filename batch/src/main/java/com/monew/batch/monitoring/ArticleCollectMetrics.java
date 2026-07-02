package com.monew.batch.monitoring;

import com.monew.batch.article.collect.dto.ArticleCollectStepResultDto;
import com.monew.batch.article.collect.dto.ArticleSaveAndInterestLinkStepResultDto;
import com.monew.batch.article.collect.dto.ArticleSourceCollectResultDto;
import com.monew.batch.article.entity.ArticleSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleCollectMetrics {

  private static final String SOURCE_TAG = "source";
  private static final String SOURCE_ALL = "ALL";

  private final MeterRegistry meterRegistry;
  private final Map<MetricKey, AtomicLong> gauges = new ConcurrentHashMap<>();

  /**
   * Naver/RSS 같은 수집 Step이 끝날 때 source별 staging 결과를 기록합니다.(articles 테이블 최종 저장 전)
   */
  public void recordSourceCollectResult(ArticleCollectStepResultDto result) {
    String source = sourceName(result.source());
    set("monew.batch.article.collect.last.staged.count", source, result.stagedCount());
    set("monew.batch.article.collect.last.duplicate_skipped.count", source,
        result.duplicateSkippedCount());
    set("monew.batch.article.collect.last.invalid_skipped.count", source, 0);
  }

  /**
   * staging 데이터를 실제 article 테이블에 반영한 뒤 최종 처리 결과를 기록합니다.
   * source=ALL은 전체 합계, source=NAVER/RSS_*는 출처별 마지막 실행 결과입니다.
   */
  public void recordPersistResult(ArticleSaveAndInterestLinkStepResultDto result) {
    set("monew.batch.article.collect.last.staged.count", SOURCE_ALL, result.stagedCount());
    set("monew.batch.article.collect.last.saved.count", SOURCE_ALL, result.savedCount());
    set("monew.batch.article.collect.last.duplicate_skipped.count", SOURCE_ALL,
        result.duplicateSkippedCount());
    set("monew.batch.article.collect.last.invalid_skipped.count", SOURCE_ALL,
        result.invalidSkippedCount());
    set("monew.batch.article.collect.last.article_interest_linked.count", SOURCE_ALL,
        result.articleInterestLinkedCount());

    for (ArticleSourceCollectResultDto sourceResult : result.sourceResults()) {
      set("monew.batch.article.collect.last.staged.count", sourceResult.source(),
          sourceResult.stagedCount());
      set("monew.batch.article.collect.last.saved.count", sourceResult.source(),
          sourceResult.savedCount());
      set("monew.batch.article.collect.last.duplicate_skipped.count", sourceResult.source(),
          sourceResult.duplicateSkippedCount());
      set("monew.batch.article.collect.last.invalid_skipped.count", sourceResult.source(),
          sourceResult.invalidSkippedCount());
    }
  }

  // 같은 metric name이라도 source tag가 다르면 별도 Gauge로 등록됩니다.
  private void set(String metricName, String source, long value) {
    gauges.computeIfAbsent(new MetricKey(metricName, source), key -> {
      AtomicLong gaugeValue = new AtomicLong();
      meterRegistry.gauge(key.name(), List.of(Tag.of(SOURCE_TAG, key.source())), gaugeValue);
      return gaugeValue;
    }).set(value);
  }

  private String sourceName(ArticleSource source) {
    return switch (source) {
      case NAVER -> "NAVER";
      case HANKYUNG -> "RSS_HANKYUNG";
      case CHOSUN -> "RSS_CHOSUN";
      case YEONHAP -> "RSS_YEONHAP";
    };
  }

  private record MetricKey(String name, String source) {
  }
}
