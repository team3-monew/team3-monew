package com.monew.batch.article.backup.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 backup.* 설정을 타입 안전하게 바인딩하는 설정 객체입니다.
 * 기본 저장소는 local 이며, S3 설정은 backup.storage.type=s3 일 때만 실제로 사용됩니다.
 */
@ConfigurationProperties(prefix = "backup")
public record BackupProperties(
    boolean enabled,
    Storage storage
) {

  public BackupProperties {
    if (storage == null) {
      storage = new Storage("local", new Local(Path.of(".monew/backups")),
          new S3("", "backups/articles", "ap-northeast-2"));
    }
  }

  /**
   * 백업 저장소 종류와 각 저장소별 세부 설정을 담습니다.
   */
  public record Storage(
      String type,
      Local local,
      S3 s3
  ) {

    public Storage {
      if (type == null || type.isBlank()) {
        type = "local";
      }
      if (local == null) {
        local = new Local(Path.of(".monew/backups"));
      }
      if (s3 == null) {
        s3 = new S3("", "backups/articles", "ap-northeast-2");
      }
    }
  }

  /**
   * 로컬 파일 시스템 백업 루트 경로 설정입니다.
   */
  public record Local(
      Path rootPath
  ) {

    public Local {
      if (rootPath == null) {
        rootPath = Path.of(".monew/backups");
      }
    }
  }

  /**
   * S3 업로드에 필요한 버킷, prefix, 리전 설정입니다.
   */
  public record S3(
      String bucket,
      String prefix,
      String region
  ) {

    public S3 {
      if (bucket == null) {
        bucket = "";
      }
      if (prefix == null || prefix.isBlank()) {
        prefix = "backups/articles";
      }
      if (region == null || region.isBlank()) {
        region = "ap-northeast-2";
      }
    }
  }
}
