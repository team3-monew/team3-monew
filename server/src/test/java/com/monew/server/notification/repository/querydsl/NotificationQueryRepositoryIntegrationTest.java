package com.monew.server.notification.repository.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.notification.dto.NotificationResponse;
import com.monew.server.notification.entity.Notification;
import com.monew.server.notification.entity.NotificationResourceType;
import com.monew.server.support.RepositoryTestSupport;
import com.monew.server.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@Import(NotificationQueryRepositoryImpl.class)
class NotificationQueryRepositoryIntegrationTest extends RepositoryTestSupport {

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("미확인 알림 조회 성공 - 사용자와 확인 여부로 필터링하고 createdAt, id 내림차순으로 조회한다")
    void findUnreadNotifications_success_filterAndSort() {
        // Given
        User user = saveUser("user1@test.com", "tester1");
        User otherUser = saveUser("other@test.com", "tester2");

        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 6, 10, 0);

        UUID lowId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID middleId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID highId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        saveNotification(user, lowId, false, createdAt);
        saveNotification(user, middleId, false, createdAt);
        saveNotification(user, highId, false, createdAt);

        saveNotification(user, UUID.fromString("00000000-0000-0000-0000-000000000004"), true, createdAt);
        saveNotification(otherUser, UUID.fromString("00000000-0000-0000-0000-000000000005"), false, createdAt);

        entityManager.flush();
        entityManager.clear();

        // When
        List<NotificationResponse> result = notificationQueryRepository.findUnreadNotifications(
                user.getId(),
                null,
                null,
                2
        );

        // Then
        assertThat(result).hasSize(3); // limit + 1

        assertThat(result)
                .extracting(NotificationResponse::id)
                .containsExactly(highId, middleId, lowId);

        assertThat(result)
                .allSatisfy(notification -> {
                    assertThat(notification.userId()).isEqualTo(user.getId());
                    assertThat(notification.confirmed()).isFalse();
                    assertThat(notification.resourceType()).isEqualTo("comment");
                });
    }

    @Test
    @DisplayName("미확인 알림 조회 성공 - cursor 이후 알림만 조회한다")
    void findUnreadNotifications_success_cursor() {
        // Given
        User user = saveUser("user2@test.com", "tester3");

        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 6, 10, 0);

        UUID lowId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID middleId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID highId = UUID.fromString("00000000-0000-0000-0000-000000000013");

        saveNotification(user, lowId, false, createdAt);
        saveNotification(user, middleId, false, createdAt);
        saveNotification(user, highId, false, createdAt);

        entityManager.flush();
        entityManager.clear();

        // When
        List<NotificationResponse> result = notificationQueryRepository.findUnreadNotifications(
                user.getId(),
                middleId.toString(),
                createdAt,
                10
        );

        // Then
        assertThat(result)
                .extracting(NotificationResponse::id)
                .containsExactly(lowId);
    }

    @Test
    @DisplayName("미확인 알림 개수 조회 성공 - 해당 사용자의 미확인 알림만 집계한다")
    void countUnreadNotifications_success() {
        // Given
        User user = saveUser("user3@test.com", "tester4");
        User otherUser = saveUser("other3@test.com", "tester5");

        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 6, 10, 0);

        saveNotification(user, UUID.randomUUID(), false, createdAt);
        saveNotification(user, UUID.randomUUID(), false, createdAt.plusSeconds(1));
        saveNotification(user, UUID.randomUUID(), false, createdAt.plusSeconds(2));
        saveNotification(user, UUID.randomUUID(), true, createdAt.plusSeconds(3));
        saveNotification(otherUser, UUID.randomUUID(), false, createdAt.plusSeconds(4));

        entityManager.flush();
        entityManager.clear();

        // When
        long result = notificationQueryRepository.countUnreadNotifications(user.getId());

        // Then
        assertThat(result).isEqualTo(3L);
    }

    private User saveUser(String email, String nickname) {
        User user = new User(email, nickname, "password");

        entityManager.persist(user);

        return user;
    }

    private Notification saveNotification(
            User user,
            UUID id,
            boolean confirmed,
            LocalDateTime createdAt
    ) {
        Notification notification = Notification.builder()
                .user(user)
                .content("테스트 알림")
                .resourceType(NotificationResourceType.COMMENT)
                .resourceId(UUID.randomUUID())
                .build();

        ReflectionTestUtils.setField(notification, "id", id);

        if (confirmed) {
            ReflectionTestUtils.setField(notification, "confirmed", true);
            ReflectionTestUtils.setField(notification, "confirmedAt", createdAt.plusMinutes(1));
        }

        entityManager.persist(notification);
        entityManager.flush();

        entityManager.createQuery("""
                update Notification n
                set n.createdAt = :createdAt
                where n.id = :id
                """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();

        entityManager.clear();

        return notification;
    }
}