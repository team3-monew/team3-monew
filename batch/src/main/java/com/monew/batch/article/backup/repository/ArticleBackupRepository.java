package com.monew.batch.article.backup.repository;

import com.monew.batch.article.entity.ArticleBackup;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * article_backups 테이블에 백업 실행 이력을 기록하고 조회하는 Repository 입니다.
 */
public interface ArticleBackupRepository extends JpaRepository<ArticleBackup, UUID> {

  Optional<ArticleBackup> findByBackupDate(LocalDate backupDate);

}