package com.monew.batch.common.exception.article;

import com.monew.batch.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleBackupErrorCode implements ErrorCode {

  INVALID_LOCAL_BACKUP_KEY(HttpStatus.BAD_REQUEST, "로컬 백업 파일 경로가 올바르지 않습니다."),
  LOCAL_BACKUP_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "로컬 백업 파일 저장에 실패했습니다."),

  S3_BUCKET_REQUIRED(HttpStatus.BAD_REQUEST, "S3 백업 버킷 설정이 필요합니다."),
  S3_BACKUP_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3 백업 파일 업로드에 실패했습니다."),

  BACKUP_JSON_SERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "기사 백업 JSON 변환에 실패했습니다."),
  BACKUP_HISTORY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "기사 백업 이력을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;
}
