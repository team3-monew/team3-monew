package com.monew.server.common.exception.interest;

import com.monew.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {

    INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사 정보를 찾을 수 없습니다."),
    SIMILAR_INTEREST_EXISTS(HttpStatus.CONFLICT, "유사한 관심사가 이미 존재합니다.");

    private final HttpStatus status;
    private final String message;
}