package com.monew.batch.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monew.batch.article.event.InterestMatchedArticleEvent;
import com.monew.batch.notification.entity.Notification;
import com.monew.batch.notification.entity.NotificationResourceType;
import com.monew.batch.notification.repository.NotificationRepository;
import com.monew.batch.subscription.repository.SubscriptionRepository;
import com.monew.batch.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InterestMatchedArticleEventListenerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InterestMatchedArticleEventListener listener;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationsCaptor;

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 성공 - 관심사별 중복 제거된 기사 수로 구독자에게 알림을 저장한다")
    void handle_success_groupsByInterestAndCreatesNotifications() {
        // Given
        UUID firstArticleId = UUID.randomUUID();
        UUID secondArticleId = UUID.randomUUID();
        UUID thirdArticleId = UUID.randomUUID();

        UUID aiInterestId = UUID.randomUUID();
        UUID javaInterestId = UUID.randomUUID();

        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        User firstUser = mock(User.class);
        User secondUser = mock(User.class);

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(firstArticleId, aiInterestId, "인공지능"),
                new InterestMatchedArticleEvent.InterestMatchData(firstArticleId, aiInterestId, "인공지능"),
                new InterestMatchedArticleEvent.InterestMatchData(secondArticleId, aiInterestId, "인공지능"),
                new InterestMatchedArticleEvent.InterestMatchData(thirdArticleId, javaInterestId, "자바")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(eq(aiInterestId), any(Pageable.class)))
                .thenReturn(List.of(firstUserId, secondUserId));
        when(subscriptionRepository.findUserIdsByInterestId(eq(javaInterestId), any(Pageable.class)))
                .thenReturn(List.of(firstUserId));

        when(entityManager.getReference(User.class, firstUserId)).thenReturn(firstUser);
        when(entityManager.getReference(User.class, secondUserId)).thenReturn(secondUser);

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository, times(2)).saveAll(notificationsCaptor.capture());

        List<Notification> notifications = notificationsCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .toList();

        assertThat(notifications).hasSize(3);
        assertThat(notifications)
                .extracting(Notification::getResourceType)
                .containsOnly(NotificationResourceType.INTEREST);
        assertThat(notifications)
                .extracting(Notification::getResourceId)
                .containsExactly(aiInterestId, aiInterestId, javaInterestId);
        assertThat(notifications)
                .extracting(Notification::getContent)
                .containsExactly(
                        "인공지능와 관련된 기사가 2건 등록되었습니다.",
                        "인공지능와 관련된 기사가 2건 등록되었습니다.",
                        "자바와 관련된 기사가 1건 등록되었습니다."
                );
        assertThat(notifications)
                .extracting(Notification::getUser)
                .containsExactly(firstUser, secondUser, firstUser);
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 생략 - 이벤트가 null이면 아무 작업도 하지 않는다")
    void handle_returns_whenEventIsNull() {
        // Given
        InterestMatchedArticleEvent event = null;

        // When
        listener.handle(event);

        // Then
        verify(subscriptionRepository, never())
                .findUserIdsByInterestId(any(), any(Pageable.class));
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 생략 - 관심사 목록이 null이면 아무 작업도 하지 않는다")
    void handle_returns_whenInterestsIsNull() {
        // Given
        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(null);

        // When
        listener.handle(event);

        // Then
        verify(subscriptionRepository, never())
                .findUserIdsByInterestId(any(), any(Pageable.class));
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 생략 - 관심사 목록이 비어 있으면 아무 작업도 하지 않는다")
    void handle_returns_whenInterestsIsEmpty() {
        // Given
        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of());

        // When
        listener.handle(event);

        // Then
        verify(subscriptionRepository, never())
                .findUserIdsByInterestId(any(), any(Pageable.class));
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 성공 - null 데이터와 articleId 또는 interestId 없는 데이터는 제외한다")
    void handle_filtersInvalidInterestData() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(Arrays.asList(
                null,
                new InterestMatchedArticleEvent.InterestMatchData(null, interestId, "무효"),
                new InterestMatchedArticleEvent.InterestMatchData(articleId, null, "무효"),
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "백엔드")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(eq(interestId), any(Pageable.class)))
                .thenReturn(List.of(userId));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        List<Notification> notifications = notificationsCaptor.getValue();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getResourceType()).isEqualTo(NotificationResourceType.INTEREST);
        assertThat(notifications.get(0).getResourceId()).isEqualTo(interestId);
        assertThat(notifications.get(0).getContent())
                .isEqualTo("백엔드와 관련된 기사가 1건 등록되었습니다.");
        assertThat(notifications.get(0).getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 생략 - 구독자가 없으면 알림을 저장하지 않는다")
    void handle_doesNotSave_whenNoSubscriber() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "인공지능")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(eq(interestId), any(Pageable.class)))
                .thenReturn(List.of());

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 성공 - 같은 관심사에 같은 기사가 중복되어도 1건으로 카운트한다")
    void handle_countsDuplicatedArticleOnlyOnceInSameInterest() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "백엔드"),
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "백엔드")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(eq(interestId), any(Pageable.class)))
                .thenReturn(List.of(userId));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        List<Notification> notifications = notificationsCaptor.getValue();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getContent())
                .isEqualTo("백엔드와 관련된 기사가 1건 등록되었습니다.");
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 성공 - 같은 관심사에 서로 다른 기사는 각각 카운트한다")
    void handle_countsDifferentArticlesInSameInterest() {
        // Given
        UUID firstArticleId = UUID.randomUUID();
        UUID secondArticleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(firstArticleId, interestId, "백엔드"),
                new InterestMatchedArticleEvent.InterestMatchData(secondArticleId, interestId, "백엔드")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(eq(interestId), any(Pageable.class)))
                .thenReturn(List.of(userId));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        List<Notification> notifications = notificationsCaptor.getValue();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getContent())
                .isEqualTo("백엔드와 관련된 기사가 2건 등록되었습니다.");
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 성공 - 구독자 조회와 알림 저장을 청크 단위로 처리한다")
    void handle_savesNotificationsByChunk() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(articleId, interestId, "백엔드")
        ));

        List<UUID> firstChunkUserIds = createUserIds(500);
        List<UUID> secondChunkUserIds = createUserIds(1);

        Map<UUID, User> usersById = new LinkedHashMap<>();
        for (UUID userId : firstChunkUserIds) {
            usersById.put(userId, mock(User.class));
        }
        for (UUID userId : secondChunkUserIds) {
            usersById.put(userId, mock(User.class));
        }

        when(subscriptionRepository.findUserIdsByInterestId(eq(interestId), any(Pageable.class)))
                .thenReturn(firstChunkUserIds, secondChunkUserIds);

        when(entityManager.getReference(eq(User.class), any(UUID.class)))
                .thenAnswer(invocation -> usersById.get(invocation.getArgument(1)));

        // When
        listener.handle(event);

        // Then
        verify(subscriptionRepository, times(2))
                .findUserIdsByInterestId(eq(interestId), any(Pageable.class));
        verify(notificationRepository, times(2)).saveAll(notificationsCaptor.capture());

        List<List<Notification>> savedChunks = notificationsCaptor.getAllValues();

        assertThat(savedChunks).hasSize(2);
        assertThat(savedChunks.get(0)).hasSize(500);
        assertThat(savedChunks.get(1)).hasSize(1);

        List<Notification> allNotifications = savedChunks.stream()
                .flatMap(List::stream)
                .toList();

        assertThat(allNotifications).hasSize(501);
        assertThat(allNotifications)
                .extracting(Notification::getResourceType)
                .containsOnly(NotificationResourceType.INTEREST);
        assertThat(allNotifications)
                .extracting(Notification::getResourceId)
                .containsOnly(interestId);
        assertThat(allNotifications)
                .extracting(Notification::getContent)
                .containsOnly("백엔드와 관련된 기사가 1건 등록되었습니다.");
    }

    private List<UUID> createUserIds(int count) {
        List<UUID> userIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            userIds.add(UUID.randomUUID());
        }

        return userIds;
    }
}