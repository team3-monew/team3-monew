package com.monew.batch.article.backup.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.batch.article.backup.config.BackupProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * LocalArticleBackupStorage 가 key 경로에 맞춰 실제 파일을 생성하는지 검증합니다.
 */
class LocalArticleBackupStorageTest {

  @TempDir
  Path tempDir;

  /**
   * root-path 아래 날짜별 백업 경로에 JSON 파일이 저장되는지 확인합니다.
   */
  @Test
  void uploadCreatesBackupFile() throws Exception {
    BackupProperties properties = new BackupProperties(true,
        new BackupProperties.Storage("local",
            new BackupProperties.Local(tempDir),
            new BackupProperties.S3("", "backups/articles", "ap-northeast-2")));
    LocalArticleBackupStorage storage = new LocalArticleBackupStorage(properties);

    storage.upload("backups/articles/date=2026-06-29/articles-2026-06-29.json",
        "[{\"title\":\"news\"}]".getBytes());

    Path backupFile =
        tempDir.resolve("backups/articles/date=2026-06-29/articles-2026-06-29.json");
    assertThat(Files.readString(backupFile)).isEqualTo("[{\"title\":\"news\"}]");
  }
}
