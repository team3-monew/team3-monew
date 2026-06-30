package com.monew.batch.article.collect.repository;

import com.monew.batch.article.collect.entity.ArticleCollectStaging;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleCollectStagingRepository extends JpaRepository<ArticleCollectStaging, UUID> {

  /**
   * 현재 배치 실행에서 이미 staging에 들어간 원문 URL인지 확인합니다.
   */
  boolean existsByJobExecutionIdAndSourceUrl(Long jobExecutionId, String sourceUrl);

  /**
   * 현재 배치 실행에서 staging에 쌓인 모든 저장 후보 기사를 조회합니다.
   */
  List<ArticleCollectStaging> findAllByJobExecutionId(Long jobExecutionId);

  List<ArticleCollectStaging> findAllByJobExecutionIdAndSourceUrlIn(Long jobExecutionId,
      Collection<String> sourceUrls);

  long deleteByJobExecutionId(Long jobExecutionId);
}
