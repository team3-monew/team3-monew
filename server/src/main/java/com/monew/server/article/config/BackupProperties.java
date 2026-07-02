package com.monew.server.article.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backup")
public record BackupProperties(
    Storage storage
) {

  public BackupProperties {
    if (storage == null) {
      storage = new Storage("local", new Local(Path.of(".monew/backups")),
          new S3("", "backups/articles", "ap-northeast-2"));
    }
  }

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

  public record Local(
      Path rootPath
  ) {

    public Local {
      if (rootPath == null) {
        rootPath = Path.of(".monew/backups");
      }
    }
  }

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
