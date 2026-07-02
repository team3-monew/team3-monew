package com.monew.server.subscription.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.interest.InterestErrorCode;
import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.interest.repository.InterestKeywordRepository;
import com.monew.server.interest.repository.InterestRepository;
import com.monew.server.subscription.dto.SubscriptionDto;
import com.monew.server.subscription.entity.Subscription;
import com.monew.server.subscription.repository.SubscriptionRepository;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// event publisher 관련 import 추가
import com.monew.server.activity.event.SubscriptionCreatedEvent;
import com.monew.server.activity.event.SubscriptionDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final InterestRepository interestRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final UserRepository userRepository;

    // event publisher 관련 필드 추가
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SubscriptionDto subscribe(UUID userId, UUID interestId) {
        Interest interest = getInterest(interestId);

        // event publisher 용 코드
        Optional<Subscription> existingSubscription =
            subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId);

        boolean created = existingSubscription.isEmpty();
        //

        Subscription subscription = subscriptionRepository
                .findByUser_IdAndInterest_Id(userId, interestId)
                .orElseGet(() -> createSubscription(userId, interest));

        List<String> keywords = interestKeywordRepository.findAllByInterestId(interestId).stream()
                .map(InterestKeyword::getKeyword)
                .toList();

        // event publisher 삽입
        if (created) {
            eventPublisher.publishEvent(
                new SubscriptionCreatedEvent(
                    userId,
                    subscription.getId(),
                    interest.getId(),
                    interest.getName(),
                    keywords,
                    interest.getSubscriberCount(),
                    subscription.getCreatedAt()
                )
            );
        }

        return SubscriptionDto.of(subscription, interest, keywords);
    }

    @Override
    @Transactional
    public void unsubscribe(UUID userId, UUID interestId) {
        Interest interest = getInterest(interestId);

        subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId)
                .ifPresent(subscription -> {

                    // event publisher용
                    UUID subscriptionId = subscription.getId();

                    subscriptionRepository.delete(subscription);
                    interest.decreaseSubscriberCount();

                    // event publisher 삽입
                    eventPublisher.publishEvent(
                        new SubscriptionDeletedEvent(
                            userId,
                            subscriptionId
                        )
                    );
                });
    }

    private Subscription createSubscription(UUID userId, Interest interest) {
        User user = userRepository.getReferenceById(userId);

        Subscription subscription = new Subscription(user, interest);
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        interest.increaseSubscriberCount();
        return savedSubscription;
    }

    private Interest getInterest(UUID interestId) {
        return interestRepository.findById(interestId)
                .orElseThrow(() -> new BaseException(InterestErrorCode.INTEREST_NOT_FOUND));
    }
}
