package com.monew.batch.common.exception.article;

import com.monew.batch.common.exception.BaseException;

public class ArticleCollectException extends BaseException {

  public ArticleCollectException(ArticleCollectErrorCode errorCode) {
    super(errorCode);
  }

  public ArticleCollectException(ArticleCollectErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
