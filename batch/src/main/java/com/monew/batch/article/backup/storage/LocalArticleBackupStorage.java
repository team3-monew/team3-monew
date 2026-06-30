package com.monew.batch.article.backup.storage;

import com.monew.batch.article.backup.config.BackupProperties;
import com.monew.batch.common.exception.article.ArticleBackupException;
import com.monew.batch.common.exception.article.ArticleBackupErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발/test 기본 백업 저장소입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "backup.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalArticleBackupStorage implements ArticleBackupStorage {

  private final BackupProperties backupProperties;

  @Override
  public void upload(String key, byte[] data) {
    Path rootPath = backupProperties.storage().local().rootPath().normalize();
    Path targetPath = rootPath.resolve(key).normalize();

    if (!targetPath.startsWith(rootPath)) {
      ArticleBackupException exception =
          new ArticleBackupException(ArticleBackupErrorCode.INVALID_LOCAL_BACKUP_KEY);
      exception.addDetail("key", key);

      throw exception;
    }

    try {

      Files.createDirectories(targetPath.getParent());
      Files.write(targetPath, data);
      log.info("[article backup] 로컬 파일에 뉴스 기사 백업. path={}", targetPath);

    } catch (IOException ex) {
      ArticleBackupException exception =
          new ArticleBackupException(ArticleBackupErrorCode.LOCAL_BACKUP_SAVE_FAILED, ex);
      exception.addDetail("path", targetPath);

      throw exception;
    }
  }

  @Override
  public String bucketName() {
    return "local";
  }
}
