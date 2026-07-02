package com.monew.batch.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monew.batch.article.event.InterestMatchedArticleEvent;
import com.monew.batch.notification.entity.Notification;
import com.monew.batch.notification.entity.NotificationResourceType;
import com.monew.batch.notification.repository.NotificationRepository;
import com.monew.batch.subscription.repository.SubscriptionRepository;
import com.monew.batch.user.entity.User;
import jakarta.persistence.EntityManager;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @DisplayName("관심사 매칭 이벤트 처리 성공 - 관심사별 기사 수를 묶어서 구독자에게 알림을 저장한다")
    void handle_success_groupsByInterestAndCreatesNotifications() {
        // Given
        UUID aiInterestId = UUID.randomUUID();
        UUID javaInterestId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        User firstUser = mock(User.class);
        User secondUser = mock(User.class);

        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(aiInterestId, "인공지능"),
                new InterestMatchedArticleEvent.InterestMatchData(aiInterestId, "인공지능"),
                new InterestMatchedArticleEvent.InterestMatchData(javaInterestId, "자바")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(aiInterestId)).thenReturn(List.of(firstUserId, secondUserId));
        when(subscriptionRepository.findUserIdsByInterestId(javaInterestId)).thenReturn(List.of(firstUserId));
        when(entityManager.getReference(User.class, firstUserId)).thenReturn(firstUser);
        when(entityManager.getReference(User.class, secondUserId)).thenReturn(secondUser);

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> notifications = notificationsCaptor.getValue();

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
        verify(subscriptionRepository, never()).findUserIdsByInterestId(any());
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
        verify(subscriptionRepository, never()).findUserIdsByInterestId(any());
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
        verify(subscriptionRepository, never()).findUserIdsByInterestId(any());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 성공 - null 데이터와 interestId 없는 데이터는 제외한다")
    void handle_filtersInvalidInterestData() {
        // Given
        UUID interestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(Arrays.asList(
                null,
                new InterestMatchedArticleEvent.InterestMatchData(null, "무효"),
                new InterestMatchedArticleEvent.InterestMatchData(interestId, "백엔드")
        ));

        when(subscriptionRepository.findUserIdsByInterestId(interestId)).thenReturn(List.of(userId));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> notifications = notificationsCaptor.getValue();

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getResourceId()).isEqualTo(interestId);
        assertThat(notifications.get(0).getContent()).isEqualTo("백엔드와 관련된 기사가 1건 등록되었습니다.");
    }

    @Test
    @DisplayName("관심사 매칭 이벤트 처리 생략 - 구독자가 없으면 알림을 저장하지 않는다")
    void handle_doesNotSave_whenNoSubscriber() {
        // Given
        UUID interestId = UUID.randomUUID();
        InterestMatchedArticleEvent event = new InterestMatchedArticleEvent(List.of(
                new InterestMatchedArticleEvent.InterestMatchData(interestId, "인공지능")
        ));
        when(subscriptionRepository.findUserIdsByInterestId(interestId)).thenReturn(List.of());

        // When
        listener.handle(event);

        // Then
        verify(notificationRepository, never()).saveAll(any());
    }
}
