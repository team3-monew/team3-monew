package com.monew.server.common.exception.notification;

import com.monew.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림 정보를 찾을 수 없습니다."),
    INVALID_NOTIFICATION_REQUEST(HttpStatus.BAD_REQUEST, "알림 요청 값이 올바르지 않습니다."),
    INVALID_NOTIFICATION_CURSOR(HttpStatus.BAD_REQUEST, "알림 커서 값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}