package com.monew.server.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.dto.UserActivityResponse;
import com.monew.server.activity.repository.UserActivityRepository;
import com.monew.server.common.exception.activity.UserActivityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserActivityServiceTest {

  private final UserActivityRepository userActivityRepository =
      Mockito.mock(UserActivityRepository.class);

  private final UserActivityService userActivityService =
      new UserActivityService(userActivityRepository);

  @Test
  void 사용자_활동내역을_조회한다() {
    String userId = "user-id";
    LocalDateTime createdAt = LocalDateTime.now();

    UserActivity activity = UserActivity.create(
        userId,
        "test@monew.com",
        "테스터",
        createdAt
    );

    when(userActivityRepository.findById(userId))
        .thenReturn(Optional.of(activity));

    UserActivityResponse response = userActivityService.getUserActivity(userId);

    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("test@monew.com");
    assertThat(response.nickname()).isEqualTo("테스터");
    assertThat(response.createdAt()).isEqualTo(createdAt);
  }

  @Test
  void 사용자_활동내역이_없으면_예외를_던진다() {
    String userId = "missing-user-id";

    when(userActivityRepository.findById(userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> userActivityService.getUserActivity(userId))
        .isInstanceOf(UserActivityNotFoundException.class);
  }
}