package com.monew.batch.article.backup.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monew.batch.article.backup.config.BackupProperties;
import com.monew.batch.common.exception.article.ArticleBackupErrorCode;
import com.monew.batch.common.exception.article.ArticleBackupException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalArticleBackupStorageTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("로컬 기사 백업 저장 성공 - rootPath 아래 날짜별 JSON 파일을 생성한다")
  void uploadCreatesBackupFile() throws Exception {
    // given
    LocalArticleBackupStorage storage = storage();
    String key = "backups/articles/date=2026-06-29/articles-2026-06-29.json";
    byte[] data = "[{\"title\":\"news\"}]".getBytes();

    // when
    storage.upload(key, data);

    // then
    Path backupFile = tempDir.resolve(key);
    assertThat(Files.readString(backupFile)).isEqualTo("[{\"title\":\"news\"}]");
  }

  @Test
  @DisplayName("로컬 기사 백업 저장 성공 - 부모 디렉터리가 없어도 중첩 디렉터리를 자동 생성한다")
  void uploadCreatesNestedDirectories() throws Exception {
    // given
    LocalArticleBackupStorage storage = storage();
    String key = "backups/articles/date=2026-06-29/articles.json";
    Path backupFile = tempDir.resolve(key);
    assertThat(backupFile.getParent()).doesNotExist();

    // when
    storage.upload(key, "[{\"title\":\"nested\"}]".getBytes());

    // then
    assertThat(backupFile.getParent()).exists().isDirectory();
    assertThat(backupFile).exists().isRegularFile();
    assertThat(Files.readString(backupFile)).isEqualTo("[{\"title\":\"nested\"}]");
  }

  @Test
  @DisplayName("로컬 기사 백업 저장 실패 - rootPath 밖으로 벗어나는 key는 INVALID_LOCAL_BACKUP_KEY 예외를 던진다")
  void uploadRejectsPathTraversalKey() {
    // given
    LocalArticleBackupStorage storage = storage();
    String key = "../escape.json";
    Path escapedFile = tempDir.resolveSibling("escape.json");

    // when & then
    assertThatThrownBy(() -> storage.upload(key, "escape".getBytes()))
        .isInstanceOf(ArticleBackupException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleBackupErrorCode.INVALID_LOCAL_BACKUP_KEY);
    assertThat(escapedFile).doesNotExist();
  }

  @Test
  @DisplayName("로컬 기사 백업 저장 성공 - 같은 key로 다시 저장하면 기존 파일을 덮어쓴다")
  void uploadOverwritesExistingFile() throws Exception {
    // given
    LocalArticleBackupStorage storage = storage();
    String key = "backups/articles/date=2026-06-29/articles.json";
    Path backupFile = tempDir.resolve(key);

    storage.upload(key, "[{\"title\":\"old\"}]".getBytes());

    // when
    storage.upload(key, "[{\"title\":\"new\"}]".getBytes());

    // then
    assertThat(Files.readString(backupFile)).isEqualTo("[{\"title\":\"new\"}]");
  }

  private LocalArticleBackupStorage storage() {
    BackupProperties properties = new BackupProperties(true,
        new BackupProperties.Storage("local",
            new BackupProperties.Local(tempDir),
            new BackupProperties.S3("", "backups/articles", "ap-northeast-2")));
    return new LocalArticleBackupStorage(properties);
  }
}
