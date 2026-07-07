package com.monew.batch.article.backup.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.monew.batch.article.backup.config.BackupProperties;
import com.monew.batch.common.exception.article.ArticleBackupErrorCode;
import com.monew.batch.common.exception.article.ArticleBackupException;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3ArticleBackupStorageTest {

  private static final String BUCKET = "monew-backup";
  private static final String KEY = "backups/articles/date=2026-06-29/articles-2026-06-29.json";

  @Mock
  private S3Client s3Client;

  @Test
  @DisplayName("S3 기사 백업 저장 성공 - bucket, key, contentType을 지정해 JSON bytes를 업로드한다")
  void uploadPutsJsonObjectToS3() throws Exception {
    // given
    S3ArticleBackupStorage storage = storage(BUCKET);
    byte[] data = "[{\"title\":\"news\"}]".getBytes();
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willReturn(PutObjectResponse.builder().build());

    // when
    storage.upload(KEY, data);

    // then
    ArgumentCaptor<PutObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(PutObjectRequest.class);
    ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
    then(s3Client).should().putObject(requestCaptor.capture(), bodyCaptor.capture());

    PutObjectRequest request = requestCaptor.getValue();
    assertThat(request.bucket()).isEqualTo(BUCKET);
    assertThat(request.key()).isEqualTo(KEY);
    assertThat(request.contentType()).isEqualTo("application/json");
    assertThat(readBytes(bodyCaptor.getValue())).isEqualTo(data);
  }

  @Test
  @DisplayName("S3 기사 백업 저장 실패 - bucket이 비어 있으면 S3_BUCKET_REQUIRED 예외를 던지고 업로드하지 않는다")
  void uploadThrowsExceptionWhenBucketBlank() {
    // given
    S3ArticleBackupStorage storage = storage(" ");

    // when & then
    assertThatThrownBy(() -> storage.upload(KEY, "[]".getBytes()))
        .isInstanceOf(ArticleBackupException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleBackupErrorCode.S3_BUCKET_REQUIRED);
    then(s3Client).should(never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("S3 기사 백업 저장 실패 - S3 업로드 중 예외가 발생하면 S3_BACKUP_UPLOAD_FAILED 예외로 감싼다")
  void uploadWrapsS3Exception() {
    // given
    S3ArticleBackupStorage storage = storage(BUCKET);
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willThrow(S3Exception.builder().statusCode(500).message("s3 failed").build());

    // when & then
    assertThatThrownBy(() -> storage.upload(KEY, "[]".getBytes()))
        .isInstanceOf(ArticleBackupException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleBackupErrorCode.S3_BACKUP_UPLOAD_FAILED);
  }

  @Test
  @DisplayName("S3 기사 백업 저장소 이름 조회 - 설정된 bucket 이름을 반환한다")
  void bucketNameReturnsConfiguredBucket() {
    // given
    S3ArticleBackupStorage storage = storage(BUCKET);

    // when
    String bucketName = storage.bucketName();

    // then
    assertThat(bucketName).isEqualTo(BUCKET);
  }

  private S3ArticleBackupStorage storage(String bucket) {
    BackupProperties properties = new BackupProperties(true,
        new BackupProperties.Storage("s3",
            new BackupProperties.Local(Path.of(".monew/backups")),
            new BackupProperties.S3(bucket, "backups/articles", "ap-northeast-2")));

    S3ArticleBackupStorage storage = new S3ArticleBackupStorage(properties);
    ReflectionTestUtils.setField(storage, "s3Client", s3Client);
    return storage;
  }

  private byte[] readBytes(RequestBody requestBody) throws IOException {
    return requestBody.contentStreamProvider().newStream().readAllBytes();
  }
}
