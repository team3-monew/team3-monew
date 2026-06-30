package com.monew.server.common.exception.interest;

import com.monew.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {

    INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사 정보를 찾을 수 없습니다."),
    SIMILAR_INTEREST_EXISTS(HttpStatus.CONFLICT, "유사한 관심사가 이미 존재합니다."),

    // 목록 조회
    INVALID_INTEREST_ORDER_BY(HttpStatus.BAD_REQUEST, "지원하지 않는 관심사 정렬 기준입니다."),
    INVALID_INTEREST_DIRECTION(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬 방향입니다."),
    INVALID_INTEREST_LIMIT(HttpStatus.BAD_REQUEST, "잘못된 페이지 크기입니다."),
    INVALID_INTEREST_CURSOR(HttpStatus.BAD_REQUEST, "잘못된 커서 값입니다.");

    private final HttpStatus status;
    private final String message;
}