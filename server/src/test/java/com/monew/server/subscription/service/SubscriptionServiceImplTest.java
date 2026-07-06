package com.monew.server.subscription.service;

import com.monew.server.activity.event.SubscriptionCreatedEvent;
import com.monew.server.activity.event.SubscriptionDeletedEvent;
import com.monew.server.common.exception.BaseException;
import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.interest.repository.InterestKeywordRepository;
import com.monew.server.interest.repository.InterestRepository;
import com.monew.server.subscription.dto.SubscriptionDto;
import com.monew.server.subscription.entity.Subscription;
import com.monew.server.subscription.repository.SubscriptionRepository;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private InterestKeywordRepository interestKeywordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("관심사를 구독하면 구독 정보가 저장되고 구독자 수가 증가한다")
    void subscribe_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = new User("test@example.com", "테스트유저", "encoded-password");
        setId(user, userId);

        Interest interest = new Interest("스포츠");
        setId(interest, interestId);

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.of(interest));

        when(subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId))
                .thenReturn(Optional.empty());

        when(userRepository.getReferenceById(userId))
                .thenReturn(user);

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    setId(subscription, subscriptionId);
                    setCreatedAt(subscription, LocalDateTime.now());
                    return subscription;
                });

        when(interestKeywordRepository.findAllByInterestId(interestId))
                .thenReturn(List.of(
                        new InterestKeyword(interest, "축구"),
                        new InterestKeyword(interest, "야구")
                ));

        // when
        SubscriptionDto response = subscriptionService.subscribe(userId, interestId);

        // then
        assertThat(interest.getSubscriberCount()).isEqualTo(1L);
        assertThat(response.interestName()).isEqualTo("스포츠");
        assertThat(response.interestKeywords()).containsExactly("축구", "야구");
        assertThat(response.interestSubscriberCount()).isEqualTo(1L);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(eventPublisher).publishEvent(any(SubscriptionCreatedEvent.class));
    }

    @Test
    @DisplayName("이미 구독 중이면 구독자 수가 중복 증가하지 않는다")
    void subscribe_alreadySubscribed() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = new User("test@example.com", "테스트유저", "encoded-password");
        setId(user, userId);

        Interest interest = new Interest("스포츠");
        setId(interest, interestId);

        Subscription existingSubscription = new Subscription(user, interest);
        setId(existingSubscription, subscriptionId);
        setCreatedAt(existingSubscription, LocalDateTime.now());

        when(interestRepository.findById(interestId))
                .thenReturn((Optional.of(interest)));

        when(subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId))
                .thenReturn(Optional.of(existingSubscription));

        when(interestKeywordRepository.findAllByInterestId(interestId))
                .thenReturn(List.of(new InterestKeyword(interest, "축구")));

        // when
        SubscriptionDto response = subscriptionService.subscribe(userId, interestId);

        // then
        assertThat(interest.getSubscriberCount()).isZero();
        assertThat(response.interestName()).isEqualTo("스포츠");
        assertThat(response.interestSubscriberCount()).isZero();

        verify(subscriptionRepository, never()).save(any(Subscription.class));
        verify(userRepository, never()).getReferenceById(userId);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("구독을 취소하면 구독 정보가 삭제되고 구독자 수가 감소한다")
    void unsubscribe_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = new User("test@example.com", "테스트 유저", "encoded-password");
        setId(user, userId);

        Interest interest = new Interest("스포츠");
        setId(interest, interestId);
        interest.increaseSubscriberCount();

        Subscription subscription = new Subscription(user, interest);
        setId(subscription, subscriptionId);
        setCreatedAt(subscription, LocalDateTime.now());

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.of(interest));

        when(subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId))
                .thenReturn(Optional.of(subscription));

        // when
        subscriptionService.unsubscribe(userId, interestId);

        // then
        assertThat(interest.getSubscriberCount()).isZero();
        verify(subscriptionRepository).delete(subscription);
        verify(eventPublisher).publishEvent(any(SubscriptionDeletedEvent.class));
    }

    @Test
    @DisplayName("구독하지 않은 관심사를 구독 취소해도 예외 없이 종료된다")
    void unsubscribe_notSubscribed() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        Interest interest = new Interest("스포츠");
        setId(interest, interestId);

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.of(interest));

        when(subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId))
                .thenReturn(Optional.empty());

        // when
        subscriptionService.unsubscribe(userId, interestId);

        // then
        assertThat(interest.getSubscriberCount()).isZero();
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 구독하면 예외가 발생한다")
    void subscribe_interestNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.subscribe(userId, interestId))
                .isInstanceOf(BaseException.class);

        verify(subscriptionRepository, never())
                .findByUser_IdAndInterest_Id(userId, interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 구독 취소하면 예외가 발생한다")
    void unsubscribe_interestNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        when(interestRepository.findById(interestId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> subscriptionService.unsubscribe(userId, interestId))
                .isInstanceOf(BaseException.class);

        verify(subscriptionRepository, never())
                .findByUser_IdAndInterest_Id(userId, interestId);
    }

    private void setId(Object target, UUID id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
    }
}
