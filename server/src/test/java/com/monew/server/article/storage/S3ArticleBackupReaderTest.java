package com.monew.server.article.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.monew.server.article.config.BackupProperties;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.article.ArticleErrorCode;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3ArticleBackupReaderTest {

  private static final String BUCKET = "monew-backup";
  private static final String KEY = "backups/articles/date=2026-07-01/articles-2026-07-01.json";

  @Mock
  S3Client s3Client;

  @Test
  @DisplayName("성공 - download가 S3 bytes를 반환한다")
  void download_success_returnS3Bytes() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(BUCKET);
    byte[] bytes = "[{\"title\":\"복구 기사\"}]".getBytes();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
        GetObjectResponse.builder().build(),
        bytes
    );
    given(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).willReturn(responseBytes);

    // when
    byte[] result = reader.download(KEY);

    // then
    assertThat(result).isEqualTo(bytes);
  }

  @Test
  @DisplayName("실패 - download 중 예외가 발생하면 백업 저장소 접근 실패 예외가 발생한다")
  void download_fail_s3Exception() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(BUCKET);
    given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
        .willThrow(S3Exception.builder().statusCode(500).build());

    // when & then
    assertThatThrownBy(() -> reader.download(KEY))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.ARTICLE_BACKUP_STORAGE_ACCESS_FAILED);
  }

  @Test
  @DisplayName("성공 - exists에서 headObject가 성공하면 true를 반환한다")
  void exists_success_headObjectSucceeded() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(BUCKET);
    given(s3Client.headObject(any(HeadObjectRequest.class)))
        .willReturn(HeadObjectResponse.builder().build());

    // when
    boolean result = reader.exists(KEY);

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("성공 - exists에서 NoSuchKeyException이 발생하면 false를 반환한다")
  void exists_success_noSuchKeyException() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(BUCKET);
    given(s3Client.headObject(any(HeadObjectRequest.class)))
        .willThrow(NoSuchKeyException.builder().build());

    // when
    boolean result = reader.exists(KEY);

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("성공 - exists에서 404 S3Exception이 발생하면 false를 반환한다")
  void exists_success_s3ExceptionStatus404() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(BUCKET);
    given(s3Client.headObject(any(HeadObjectRequest.class)))
        .willThrow(S3Exception.builder().statusCode(404).build());

    // when
    boolean result = reader.exists(KEY);

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("실패 - bucket이 blank이면 잘못된 기사 요청 예외가 발생한다")
  void exists_fail_bucketBlank() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(" ");

    // when & then
    assertThatThrownBy(() -> reader.exists(KEY))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);

    then(s3Client).should(never()).headObject(any(HeadObjectRequest.class));
  }

  @Test
  @DisplayName("실패 - exists에서 500 S3Exception이 발생하면 백업 저장소 접근 실패 예외가 발생한다")
  void exists_fail_s3ExceptionStatus500() {
    // given
    S3ArticleBackupReader reader = s3ArticleBackupReader(BUCKET);
    given(s3Client.headObject(any(HeadObjectRequest.class)))
        .willThrow(S3Exception.builder().statusCode(500).build());

    // when & then
    assertThatThrownBy(() -> reader.exists(KEY))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.ARTICLE_BACKUP_STORAGE_ACCESS_FAILED);
  }

  private S3ArticleBackupReader s3ArticleBackupReader(String bucket) {
    BackupProperties backupProperties = new BackupProperties(
        new BackupProperties.Storage(
            "s3",
            new BackupProperties.Local(Path.of(".monew/backups")),
            new BackupProperties.S3(bucket, "backups/articles", "ap-northeast-2")
        )
    );

    return new S3ArticleBackupReader(backupProperties, s3Client);
  }
}
