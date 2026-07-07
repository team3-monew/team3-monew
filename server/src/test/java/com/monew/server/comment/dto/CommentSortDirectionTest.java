package com.monew.server.comment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommentSortDirectionTest {

  @ParameterizedTest
  @CsvSource({
      "asc, ASC",
      "ASC, ASC",
      "desc, DESC",
      "DESC, DESC"
  })
  @DisplayName("from 성공 - 대소문자와 무관하게 올바른 Enum으로 매핑한다")
  void from_success_caseInsensitive(String input, CommentSortDirection expected) {
    // when
    CommentSortDirection result = CommentSortDirection.from(input);

    // then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("from 성공 - null이면 기본값(DESC)을 반환한다")
  void from_success_nullReturnsDefault() {
    // when
    CommentSortDirection result = CommentSortDirection.from(null);

    // then
    assertThat(result).isEqualTo(CommentSortDirection.DESC);
  }

  @Test
  @DisplayName("from 실패 - 지원하지 않는 값이면 예외가 발생한다")
  void from_fail_unsupportedValue() {
    // when & then
    assertThatThrownBy(() -> CommentSortDirection.from("UP"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}