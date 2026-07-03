package com.monew.server.comment.dto;

public enum CommentSortDirection {
  ASC,
  DESC;

  public static CommentSortDirection from(String value) {
    if (value == null) {
      return DESC; // 기본값
    }
    try {
      return CommentSortDirection.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("지원하지 않는 정렬 방향입니다: " + value);
    }
  }
}