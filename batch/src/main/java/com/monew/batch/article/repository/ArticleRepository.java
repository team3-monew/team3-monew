package com.monew.batch.article.repository;

import com.monew.batch.article.entity.Article;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

  Optional<Article> findBySourceUrl(String sourceUrl);

  List<Article> findAllBySourceUrlIn(Collection<String> sourceUrls);

  List<Article> findAllByPublishDateGreaterThanEqualAndPublishDateLessThan(
      LocalDateTime start,
      LocalDateTime end
  );
}
