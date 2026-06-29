package com.monew.batch.article.backup.exception;

public class ArticleBackupException extends RuntimeException {

  public ArticleBackupException(String message) {
    super(message);
  }

  public ArticleBackupException(String message, Throwable cause) {
    super(message, cause);
  }
}
