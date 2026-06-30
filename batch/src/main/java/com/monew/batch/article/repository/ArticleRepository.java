package com.monew.batch.article.repository;

import com.monew.batch.article.entity.Article;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * articles 테이블에 접근하는 배치 모듈 전용 Repository입니다.
 * sourceUrl 중복 확인과 기존 기사 재사용을 위해 URL 기반 조회 메서드를 둡니다.
 */
public interface ArticleRepository extends JpaRepository<Article, UUID> {

  /**
   * 기사 원문 URL은 unique 값이므로, 이미 저장된 기사인지 확인할 때 사용합니다.
   */
  Optional<Article> findBySourceUrl(String sourceUrl);

  /**
   * API에서 가져온 여러 URL 중 DB에 이미 존재하는 기사를 한 번에 조회합니다.
   */
  List<Article> findAllBySourceUrlIn(Collection<String> sourceUrls);
}
