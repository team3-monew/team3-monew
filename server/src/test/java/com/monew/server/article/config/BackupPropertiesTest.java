package com.monew.server.article.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BackupPropertiesTest {

  @Test
  @DisplayName("성공 - BackupProperties에 null storage를 전달하면 기본 storage를 생성한다")
  void backupProperties_success_defaultStorageWhenStorageIsNull() {
    // given
    BackupProperties.Storage storage = null;

    // when
    BackupProperties properties = new BackupProperties(storage);

    // then
    assertThat(properties.storage()).isNotNull();
    assertThat(properties.storage().type()).isEqualTo("local");
    assertThat(properties.storage().local().rootPath()).isEqualTo(Path.of(".monew/backups"));
    assertThat(properties.storage().s3().bucket()).isEmpty();
    assertThat(properties.storage().s3().prefix()).isEqualTo("backups/articles");
    assertThat(properties.storage().s3().region()).isEqualTo("ap-northeast-2");
  }

  @Test
  @DisplayName("성공 - Storage에 null 값을 전달하면 type, local, s3 기본값을 세팅한다")
  void storage_success_defaultValuesWhenArgumentsAreNull() {
    // given
    String type = null;
    BackupProperties.Local local = null;
    BackupProperties.S3 s3 = null;

    // when
    BackupProperties.Storage storage = new BackupProperties.Storage(type, local, s3);

    // then
    assertThat(storage.type()).isEqualTo("local");
    assertThat(storage.local()).isNotNull();
    assertThat(storage.local().rootPath()).isEqualTo(Path.of(".monew/backups"));
    assertThat(storage.s3()).isNotNull();
    assertThat(storage.s3().bucket()).isEmpty();
    assertThat(storage.s3().prefix()).isEqualTo("backups/articles");
    assertThat(storage.s3().region()).isEqualTo("ap-northeast-2");
  }

  @Test
  @DisplayName("성공 - S3에 null 값을 전달하면 bucket은 빈 값으로, prefix와 region은 기본값으로 세팅한다")
  void s3_success_defaultValuesWhenArgumentsAreNull() {
    // given
    String bucket = null;
    String prefix = null;
    String region = null;

    // when
    BackupProperties.S3 s3 = new BackupProperties.S3(bucket, prefix, region);

    // then
    assertThat(s3.bucket()).isEmpty();
    assertThat(s3.prefix()).isEqualTo("backups/articles");
    assertThat(s3.region()).isEqualTo("ap-northeast-2");
  }

  @Test
  @DisplayName("성공 - S3에 빈 문자열을 전달하면 prefix와 region은 기본값으로 세팅한다")
  void s3_success_defaultPrefixAndRegionWhenArgumentsAreBlank() {
    // given
    String bucket = "";
    String prefix = "";
    String region = "";

    // when
    BackupProperties.S3 s3 = new BackupProperties.S3(bucket, prefix, region);

    // then
    assertThat(s3.bucket()).isEmpty();
    assertThat(s3.prefix()).isEqualTo("backups/articles");
    assertThat(s3.region()).isEqualTo("ap-northeast-2");
  }
}
