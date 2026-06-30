package com.monew.batch.common.exception.article;

import com.monew.batch.common.exception.BaseException;

public class ArticleBackupException extends BaseException {

  public ArticleBackupException(ArticleBackupErrorCode errorCode) {
    super(errorCode);
  }

  public ArticleBackupException(ArticleBackupErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
