package com.monew.server.article.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monew.server.article.config.BackupProperties;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.article.ArticleErrorCode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalArticleBackupReaderTest {

  @TempDir
  Path tempDir;

  @Test
  @DisplayName("성공 - 파일 내용을 download로 읽는다")
  void download_success_readFileContent() throws Exception {
    // given
    String key = "date=2026-07-01/articles-2026-07-01.json";
    String content = "[{\"title\":\"복구 기사\"}]";
    Files.createDirectories(tempDir.resolve("date=2026-07-01"));
    Files.writeString(tempDir.resolve(key), content);
    LocalArticleBackupReader reader = localArticleBackupReader(tempDir);

    // when
    byte[] result = reader.download(key);

    // then
    assertThat(result).isEqualTo(content.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("실패 - 존재하지 않는 파일을 download하면 백업 저장소 접근 실패 예외가 발생한다")
  void download_fail_fileNotExists() {
    // given
    String key = "date=2026-07-01/articles-2026-07-01.json";
    LocalArticleBackupReader reader = localArticleBackupReader(tempDir);

    // when && then
    assertThatThrownBy(() -> reader.download(key))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.ARTICLE_BACKUP_STORAGE_ACCESS_FAILED);
  }

  @Test
  @DisplayName("성공 - 파일이 있으면 exists가 true를 반환한다")
  void exists_success_fileExists() throws Exception {
    // given
    String key = "date=2026-07-01/articles-2026-07-01.json";
    Files.createDirectories(tempDir.resolve("date=2026-07-01"));
    Files.writeString(tempDir.resolve(key), "[]");
    LocalArticleBackupReader reader = localArticleBackupReader(tempDir);

    // when
    boolean result = reader.exists(key);

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("성공 - 파일이 없으면 exists가 false를 반환한다")
  void exists_success_fileNotExists() {
    // given
    String key = "date=2026-07-01/articles-2026-07-01.json";
    LocalArticleBackupReader reader = localArticleBackupReader(tempDir);

    // when
    boolean result = reader.exists(key);

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("실패 - root 밖 경로이면 잘못된 기사 요청 예외가 발생한다")
  void exists_fail_pathTraversal() {
    // given
    String key = "../outside.json";
    LocalArticleBackupReader reader = localArticleBackupReader(tempDir);

    // when && then
    assertThatThrownBy(() -> reader.exists(key))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
  }

  private LocalArticleBackupReader localArticleBackupReader(Path rootPath) {
    BackupProperties backupProperties = new BackupProperties(
        new BackupProperties.Storage(
            "local",
            new BackupProperties.Local(rootPath),
            new BackupProperties.S3("", "backups/articles", "ap-northeast-2")
        )
    );

    return new LocalArticleBackupReader(backupProperties);
  }
}
