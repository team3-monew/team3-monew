package com.monew.server.common.exception.article;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.ErrorCode;

public class ArticleException extends BaseException {

    public ArticleException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ArticleException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}