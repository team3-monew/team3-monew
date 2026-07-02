package com.monew.server.article.storage;

import com.monew.server.article.config.BackupProperties;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.article.ArticleException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "backup.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalArticleBackupReader implements ArticleBackupReader {

  private final BackupProperties backupProperties;

  @Override
  public byte[] download(String key) {
    Path path = resolve(key);

    try {

      return Files.readAllBytes(path);

    } catch (IOException ex) {

      ArticleException exception =
          new ArticleException(ArticleErrorCode.ARTICLE_BACKUP_STORAGE_ACCESS_FAILED, ex);
      exception.addDetail("path", path);
      throw exception;

    }
  }

  @Override
  public boolean exists(String key) {
    return Files.exists(resolve(key));
  }

  private Path resolve(String key) {
    Path rootPath = backupProperties.storage().local().rootPath().normalize();
    Path targetPath = rootPath.resolve(key).normalize();

    if (!targetPath.startsWith(rootPath)) {
      ArticleException exception =
          new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
      exception.addDetail("key", key);
      throw exception;
    }

    return targetPath;
  }
}
