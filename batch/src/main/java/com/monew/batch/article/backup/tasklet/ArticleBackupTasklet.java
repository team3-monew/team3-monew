package com.monew.batch.article.backup.tasklet;

import com.monew.batch.article.backup.service.ArticleBackupService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Spring Batch Step 에서 실행되는 얇은 어댑터입니다.
 * JobParameter 의 targetDate 를 파싱한 뒤 ArticleBackupService 에 실제 백업 처리를 위임합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBackupTasklet implements Tasklet {

  private final ArticleBackupService articleBackupService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String targetDateValue = chunkContext.getStepContext()
        .getJobParameters()
        .get("targetDate")
        .toString();
    LocalDate targetDate = LocalDate.parse(targetDateValue);

    log.info("Article backup tasklet started. targetDate={}", targetDate);
    articleBackupService.backup(targetDate);
    return RepeatStatus.FINISHED;
  }
}
