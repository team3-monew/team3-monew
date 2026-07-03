package com.monew.server.article.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "backup.storage.type", havingValue = "s3")
public class S3ClientConfig {

  @Bean
  public S3Client s3Client(BackupProperties backupProperties) {
    return S3Client.builder()
        .region(Region.of(backupProperties.storage().s3().region()))
        .build();
  }
}
