package com.monew.server.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationEntityTest {

    @Test
    @DisplayName("알림 생성 성공 - 빌더로 생성하면 미확인 상태가 기본값이다")
    void builder_success_defaultUnconfirmed() {
        // given
        User user = user(UUID.randomUUID());
        UUID resourceId = UUID.randomUUID();

        // when
        Notification notification = Notification.builder()
                .user(user)
                .content("알림 내용")
                .resourceType(NotificationResourceType.COMMENT)
                .resourceId(resourceId)
                .build();

        // then
        assertThat(notification.getUser()).isSameAs(user);
        assertThat(notification.getContent()).isEqualTo("알림 내용");
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.COMMENT);
        assertThat(notification.getResourceId()).isEqualTo(resourceId);
        assertThat(notification.isConfirmed()).isFalse();
        assertThat(notification.getConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("알림 저장 전 처리 성공 - id가 없으면 UUID를 생성한다")
    void prePersist_success_generateId() {
        // given
        Notification notification = notification();

        // when
        ReflectionTestUtils.invokeMethod(notification, "prePersist");

        // then
        assertThat(notification.getId()).isNotNull();
    }

    @Test
    @DisplayName("알림 저장 전 처리 성공 - 기존 id가 있으면 유지한다")
    void prePersist_success_keepExistingId() {
        // given
        UUID id = UUID.randomUUID();
        Notification notification = notification();
        ReflectionTestUtils.setField(notification, "id", id);

        // when
        ReflectionTestUtils.invokeMethod(notification, "prePersist");

        // then
        assertThat(notification.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("알림 확인 성공 - 미확인 알림이면 확인 상태와 확인 시간을 설정한다")
    void confirm_success() {
        // given
        Notification notification = notification();

        // when
        notification.confirm();

        // then
        assertThat(notification.isConfirmed()).isTrue();
        assertThat(notification.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 확인 성공 - 이미 확인된 알림이면 기존 확인 시간을 유지한다")
    void confirm_success_alreadyConfirmedKeepConfirmedAt() {
        // given
        Notification notification = notification();
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        ReflectionTestUtils.setField(notification, "confirmed", true);
        ReflectionTestUtils.setField(notification, "confirmedAt", confirmedAt);

        // when
        notification.confirm();

        // then
        assertThat(notification.isConfirmed()).isTrue();
        assertThat(notification.getConfirmedAt()).isEqualTo(confirmedAt);
    }

    private Notification notification() {
        return Notification.builder()
                .user(user(UUID.randomUUID()))
                .content("알림 내용")
                .resourceType(NotificationResourceType.COMMENT)
                .resourceId(UUID.randomUUID())
                .build();
    }

    private User user(UUID userId) {
        User user = new User("user" + userId + "@example.com", "사용자", "Password1!");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
