package com.monew.server.common.exception.article;

import com.monew.server.common.exception.BaseException;

public class ArticleException extends BaseException {

    public ArticleException(ArticleErrorCode errorCode) {
        super(errorCode);
    }
}