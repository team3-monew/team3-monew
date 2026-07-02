package com.monew.batch.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.monew.batch.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationEntityTest {

    @Test
    @DisplayName("알림 생성 성공 - 기본 상태는 미확인이고 입력값을 보관한다")
    void buildNotification_success() {
        // Given
        User user = mock(User.class);
        UUID resourceId = UUID.randomUUID();

        // When
        Notification notification = Notification.builder()
                .user(user)
                .content("인공지능와 관련된 기사가 2건 등록되었습니다.")
                .resourceType(NotificationResourceType.INTEREST)
                .resourceId(resourceId)
                .build();

        // Then
        assertThat(notification.getUser()).isSameAs(user);
        assertThat(notification.getContent()).isEqualTo("인공지능와 관련된 기사가 2건 등록되었습니다.");
        assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.INTEREST);
        assertThat(notification.getResourceId()).isEqualTo(resourceId);
        assertThat(notification.isConfirmed()).isFalse();
        assertThat(notification.getConfirmedAt()).isNull();
    }

    @Test
    @DisplayName("알림 저장 전 처리 성공 - id가 없으면 UUID를 생성한다")
    void prePersist_generatesId_whenIdIsNull() {
        // Given
        Notification notification = Notification.builder()
                .user(mock(User.class))
                .content("알림")
                .resourceType(NotificationResourceType.INTEREST)
                .resourceId(UUID.randomUUID())
                .build();

        // When
        notification.prePersist();

        // Then
        assertThat(notification.getId()).isNotNull();
    }

    @Test
    @DisplayName("알림 확인 성공 - 미확인 알림이면 확인 상태와 확인 시각을 설정한다")
    void confirm_success_whenUnconfirmed() {
        // Given
        Notification notification = Notification.builder()
                .user(mock(User.class))
                .content("알림")
                .resourceType(NotificationResourceType.INTEREST)
                .resourceId(UUID.randomUUID())
                .build();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // When
        notification.confirm();

        // Then
        assertThat(notification.isConfirmed()).isTrue();
        assertThat(notification.getConfirmedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("알림 확인 멱등성 - 이미 확인된 알림이면 확인 시각을 유지한다")
    void confirm_doesNothing_whenAlreadyConfirmed() {
        // Given
        Notification notification = Notification.builder()
                .user(mock(User.class))
                .content("알림")
                .resourceType(NotificationResourceType.INTEREST)
                .resourceId(UUID.randomUUID())
                .build();
        notification.confirm();
        LocalDateTime confirmedAt = notification.getConfirmedAt();

        // When
        notification.confirm();

        // Then
        assertThat(notification.isConfirmed()).isTrue();
        assertThat(notification.getConfirmedAt()).isEqualTo(confirmedAt);
    }
}
