package com.monew.batch.common.exception.article;

import com.monew.batch.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleCollectErrorCode implements ErrorCode {

  DB_STEP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "기사 수집 DB Step 처리에 실패했습니다."),
  DB_RETRY_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "기사 수집 DB Step 재시도 대기 중 인터럽트가 발생했습니다.");

  private final HttpStatus status;
  private final String message;
}
