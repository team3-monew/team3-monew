package com.monew.server.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationResponseTest {

    @Test
    @DisplayName("알림 응답 DTO 생성 성공 - 생성자 인자로 받은 값을 그대로 보관한다")
    void createNotificationResponse_success() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 7, 2, 10, 0);

        // When
        NotificationResponse response = new NotificationResponse(
                id,
                createdAt,
                confirmedAt,
                true,
                userId,
                "알림 내용",
                "comment",
                resourceId
        );

        // Then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.confirmedAt()).isEqualTo(confirmedAt);
        assertThat(response.confirmed()).isTrue();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.content()).isEqualTo("알림 내용");
        assertThat(response.resourceType()).isEqualTo("comment");
        assertThat(response.resourceId()).isEqualTo(resourceId);
    }

    @Test
    @DisplayName("미확인 알림 응답 DTO 생성 성공 - confirmedAt이 null이어도 생성된다")
    void createNotificationResponse_success_unconfirmed() {
        // Given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);

        // When
        NotificationResponse response = new NotificationResponse(
                id,
                createdAt,
                null,
                false,
                userId,
                "미확인 알림",
                "interest",
                resourceId
        );

        // Then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.confirmedAt()).isNull();
        assertThat(response.confirmed()).isFalse();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.content()).isEqualTo("미확인 알림");
        assertThat(response.resourceType()).isEqualTo("interest");
        assertThat(response.resourceId()).isEqualTo(resourceId);
    }
}
