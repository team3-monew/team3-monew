package com.monew.server.article.storage;

import com.monew.server.article.config.BackupProperties;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.article.ArticleException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(name = "backup.storage.type", havingValue = "s3")
public class S3ArticleBackupReader implements ArticleBackupReader {

  private final BackupProperties backupProperties;
  private final S3Client s3Client;

  public S3ArticleBackupReader(BackupProperties backupProperties) {
    this.backupProperties = backupProperties;
    this.s3Client = S3Client.builder()
        .region(Region.of(backupProperties.storage().s3().region()))
        .build();
  }

  @Override
  public byte[] download(String key) {
    String bucket = requireBucket();

    try {
      ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest
          .builder()
          .bucket(bucket)
          .key(key)
          .build());

      return response.asByteArray();

    } catch (Exception ex) {

      ArticleException exception =
          new ArticleException(ArticleErrorCode.ARTICLE_BACKUP_STORAGE_ACCESS_FAILED, ex);
      exception.addDetail("bucket", bucket);
      exception.addDetail("key", key);

      throw exception;
    }
  }

  @Override
  public boolean exists(String key) {
    String bucket = requireBucket();

    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build());

      return true;
    } catch (NoSuchKeyException ex) {
      return false;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return false;
      }

      ArticleException exception =
          new ArticleException(ArticleErrorCode.ARTICLE_BACKUP_STORAGE_ACCESS_FAILED, ex);
      exception.addDetail("bucket", bucket);
      exception.addDetail("key", key);

      throw exception;

    }
  }

  private String requireBucket() {
    String bucket = backupProperties.storage().s3().bucket();

    if (bucket == null || bucket.isBlank()) {
      ArticleException exception =
          new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
      exception.addDetail("bucket", bucket);

      throw exception;
    }

    return bucket;
  }
}
