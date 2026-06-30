package com.monew.batch.article.backup.repository;

import com.monew.batch.article.entity.ArticleBackup;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleBackupRepository extends JpaRepository<ArticleBackup, UUID> {

  Optional<ArticleBackup> findByBackupDate(LocalDate backupDate);

}