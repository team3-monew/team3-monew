package com.monew.batch.article.backup.tasklet;

import com.monew.batch.article.backup.dto.ArticleBackupResultDto;
import com.monew.batch.article.backup.service.ArticleBackupService;
import com.monew.batch.monitoring.ArticleBackupMetrics;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * JobParameter 의 targetDate 를 파싱한 뒤 ArticleBackupService 에 실제 백업 처리를 위임합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBackupTasklet implements Tasklet {

  private final ArticleBackupService articleBackupService;
  private final ArticleBackupMetrics articleBackupMetrics;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String targetDateValue = chunkContext.getStepContext()
        .getJobParameters()
        .get("targetDate")
        .toString();
    LocalDate targetDate = LocalDate.parse(targetDateValue);

    log.info("[article backup] [article backup] 기사 백업 tasklet 시작. 백업 날짜={}\"={}", targetDate);
    try {
      ArticleBackupResultDto result = articleBackupService.backup(targetDate);

      // 마지막 백업 처리 결과를 Actuator Gauge에 반영
      articleBackupMetrics.recordResult(result);
    } catch (RuntimeException ex) {
      // 백업 전용 실패 메트릭 갱신
      articleBackupMetrics.recordFailure();
      throw ex;
    }
    return RepeatStatus.FINISHED;
  }
}
