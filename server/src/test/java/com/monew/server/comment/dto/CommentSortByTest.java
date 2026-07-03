package com.monew.server.comment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommentSortByTest {

  @ParameterizedTest
  @CsvSource({
      "createdAt, CREATED_AT",
      "CreatedAt, CREATED_AT",
      "likeCount, LIKE_COUNT",
      "like, LIKE_COUNT"
  })
  @DisplayName("from 성공 - 다양한 표기의 정렬 기준 문자열을 올바른 Enum으로 매핑한다")
  void from_success_variousCasing(String input, CommentSortBy expected) {
    // when
    CommentSortBy result = CommentSortBy.from(input);

    // then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("from 성공 - null이면 기본값(CREATED_AT)을 반환한다")
  void from_success_nullReturnsDefault() {
    // when
    CommentSortBy result = CommentSortBy.from(null);

    // then
    assertThat(result).isEqualTo(CommentSortBy.CREATED_AT);
  }

  @Test
  @DisplayName("from 실패 - 지원하지 않는 값이면 예외가 발생한다")
  void from_fail_unsupportedValue() {
    // when & then
    assertThatThrownBy(() -> CommentSortBy.from("asdf"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}