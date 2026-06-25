package com.monew.batch.article.scheduler;

import com.monew.batch.article.collect.service.ArticleCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCollectScheduler {

  private final ArticleCollectService articleCollectService;

  // 매시간마다 기사 수집 로직 수행
  @Scheduled(cron = "${monew.article-collect.cron}")
  public void collectHourly() {
    log.info("Start scheduled article collect.");
    articleCollectService.collect();
  }

}
