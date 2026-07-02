package com.monew.batch.article.repository;

import com.monew.batch.article.entity.ArticleInterest;
import com.monew.batch.article.entity.ArticleInterestId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 기사와 관심사의 매핑 테이블(article_interests)에 접근하는 Repository입니다.
 * 복합키 ArticleInterestId로 중복 매핑 여부를 확인합니다.
 */
public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, ArticleInterestId> {
}
