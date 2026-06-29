package com.monew.server.common.exception.activity;

import com.monew.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ActivityErrorCode implements ErrorCode {

  USER_ACTIVITY_NOT_FOUND(
      HttpStatus.NOT_FOUND,
      "사용자 활동 내역을 찾을 수 없습니다."
  );

  private final HttpStatus status;
  private final String message;
}