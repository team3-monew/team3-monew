package com.monew.server.activity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.activity.document.UserActivity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserActivityResponseTest {

  @Test
  void UserActivity를_Response로_변환한다() {
    String userId = "user-id";
    LocalDateTime createdAt = LocalDateTime.now();

    UserActivity activity = UserActivity.create(
        userId,
        "test@monew.com",
        "테스터",
        createdAt
    );

    UserActivityResponse response = UserActivityResponse.from(activity);

    assertThat(response.id()).isEqualTo(userId);
    assertThat(response.email()).isEqualTo("test@monew.com");
    assertThat(response.nickname()).isEqualTo("테스터");
    assertThat(response.createdAt()).isEqualTo(createdAt);
    assertThat(response.subscriptions()).isEmpty();
    assertThat(response.comments()).isEmpty();
    assertThat(response.commentLikes()).isEmpty();
    assertThat(response.articleViews()).isEmpty();
  }
}