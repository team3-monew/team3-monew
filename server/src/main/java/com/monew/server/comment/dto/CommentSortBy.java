package com.monew.server.comment.dto;

// 댓글 목록 정렬 기준. 요구사항 문서상 댓글은 날짜/좋아요 수 2개만 지원.
public enum CommentSortBy {
  CREATED_AT,
  LIKE_COUNT;

  // 클라이언트가 "createdAt", "likeCount" 등 카멜케이스로 보내는 걸 매핑
  public static CommentSortBy from(String value) {
    if (value == null) {
      return CREATED_AT; // 기본값
    }
    return switch (value.toLowerCase()) {
      case "createdat" -> CREATED_AT;
      case "likecount", "like" -> LIKE_COUNT;
      default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + value);
    };
  }
}