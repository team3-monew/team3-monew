package com.monew.batch.article.backup.storage;

public interface ArticleBackupStorage {

  /**
   * 지정한 key 위치에 JSON 바이트 배열을 저장합니다.
   */
  void upload(String key, byte[] data);

  // 백업 이력에 기록할 저장소 이름. local 저장소는 "local", S3 저장소는 버킷명 반환
  String bucketName();
}
