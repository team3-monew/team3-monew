package com.monew.batch.article.backup.storage;

import com.monew.batch.article.backup.config.BackupProperties;
import com.monew.batch.article.backup.exception.ArticleBackupException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 운영 환경에서 사용할 S3 백업 저장소입니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "backup.storage.type", havingValue = "s3")
public class S3ArticleBackupStorage implements ArticleBackupStorage {

  private final BackupProperties backupProperties;
  private final S3Client s3Client;

  /**
   * 설정된 AWS region 으로 S3Client 를 생성합니다.
   * 인증 정보는 AWS SDK 기본 credential provider chain 을 따릅니다.
   */
  public S3ArticleBackupStorage(BackupProperties backupProperties) {
    this.backupProperties = backupProperties;
    this.s3Client = S3Client.builder()
        .region(Region.of(backupProperties.storage().s3().region()))
        .build();
  }

  @Override
  public void upload(String key, byte[] data) {
    String bucket = backupProperties.storage().s3().bucket();
    if (bucket == null || bucket.isBlank()) {
      throw new ArticleBackupException("backup.storage.s3.bucket is required");
    }

    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType("application/json")
          .build();
      s3Client.putObject(request, RequestBody.fromBytes(data));
      log.info("Article backup uploaded to S3. bucket={}, key={}", bucket, key);
    } catch (Exception ex) {
      throw new ArticleBackupException("Failed to upload article backup to S3", ex);
    }
  }

  @Override
  public String bucketName() {
    return backupProperties.storage().s3().bucket();
  }
}
