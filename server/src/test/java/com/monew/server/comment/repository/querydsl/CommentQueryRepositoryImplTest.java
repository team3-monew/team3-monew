package com.monew.server.comment.repository.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.dsl.BooleanExpression;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CommentQueryRepositoryImplTest {

  @Test
  @DisplayName("작성일 커서 조건 생성 성공 - lastCreatedAt이 null이면 조건을 생성하지 않는다")
    // [핵심] 이게 바로 원래 버그였던 케이스: 최초 조회(cursor 없음)에서
    // 조건이 아예 안 붙어야 InvalidDataAccessResourceUsageException이 재발하지 않음
  void createdAtCursorCondition_success_nullLastCreatedAt() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    BooleanExpression result = ReflectionTestUtils.invokeMethod(
        repository, "createdAtCursorCondition", null, UUID.randomUUID(), true
    );

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("작성일 커서 조건 생성 성공 - 오름차순 cursor가 있으면 조건을 생성한다")
  void createdAtCursorCondition_success_ascWithValue() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    BooleanExpression result = ReflectionTestUtils.invokeMethod(
        repository,
        "createdAtCursorCondition",
        LocalDateTime.of(2026, 7, 1, 10, 0),
        UUID.randomUUID(),
        true
    );

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("작성일 커서 조건 생성 성공 - 내림차순 cursor가 있으면 조건을 생성한다")
  void createdAtCursorCondition_success_descWithValue() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    BooleanExpression result = ReflectionTestUtils.invokeMethod(
        repository,
        "createdAtCursorCondition",
        LocalDateTime.of(2026, 7, 1, 10, 0),
        UUID.randomUUID(),
        false
    );

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("좋아요 수 커서 조건 생성 성공 - lastLikeCount가 null이면 조건을 생성하지 않는다")
    // [핵심] 좋아요 순 정렬에서도 동일하게 최초 조회 시 조건 미생성 검증
  void likeCountCursorCondition_success_nullLastLikeCount() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    BooleanExpression result = ReflectionTestUtils.invokeMethod(
        repository, "likeCountCursorCondition", null, UUID.randomUUID(), true
    );

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("좋아요 수 커서 조건 생성 성공 - 오름차순 cursor가 있으면 조건을 생성한다")
  void likeCountCursorCondition_success_ascWithValue() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    BooleanExpression result = ReflectionTestUtils.invokeMethod(
        repository, "likeCountCursorCondition", 5L, UUID.randomUUID(), true
    );

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("좋아요 수 커서 조건 생성 성공 - 내림차순 cursor가 있으면 조건을 생성한다")
  void likeCountCursorCondition_success_descWithValue() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    BooleanExpression result = ReflectionTestUtils.invokeMethod(
        repository, "likeCountCursorCondition", 5L, UUID.randomUUID(), false
    );

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("정렬 방향 판별 성공 - ASC는 오름차순으로 판단한다")
  void isAsc_success_asc() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    Boolean result = ReflectionTestUtils.invokeMethod(repository, "isAsc", "ASC");

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("정렬 방향 판별 성공 - asc 소문자도 오름차순으로 판단한다")
  void isAsc_success_lowercaseAsc() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    Boolean result = ReflectionTestUtils.invokeMethod(repository, "isAsc", "asc");

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("정렬 방향 판별 성공 - DESC는 내림차순으로 판단한다")
  void isAsc_success_desc() {
    // Given
    CommentQueryRepositoryImpl repository = new CommentQueryRepositoryImpl(null);

    // When
    Boolean result = ReflectionTestUtils.invokeMethod(repository, "isAsc", "DESC");

    // Then
    assertThat(result).isFalse();
  }
}