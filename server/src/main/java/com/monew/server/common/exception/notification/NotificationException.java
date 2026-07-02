package com.monew.server.common.exception.notification;

import com.monew.server.common.exception.BaseException;

public class NotificationException extends BaseException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(NotificationErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}