package com.monew.server.article.storage;

public interface ArticleBackupReader {

  byte[] download(String key);

  boolean exists(String key);
}
