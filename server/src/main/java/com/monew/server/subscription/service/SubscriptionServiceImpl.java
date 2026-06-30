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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final InterestRepository interestRepository;
    private final InterestKeywordRepository interestKeywordRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubscriptionDto subscribe(UUID userId, UUID interestId) {
        Interest interest = getInterest(interestId);

        Subscription subscription = subscriptionRepository
                .findByUser_IdAndInterest_Id(userId, interestId)
                .orElseGet(() -> createSubscription(userId, interest));

        List<String> keywords = interestKeywordRepository.findAllByInterestId(interestId).stream()
                .map(InterestKeyword::getKeyword)
                .toList();

        return SubscriptionDto.of(subscription, interest, keywords);
    }

    @Override
    public void unsubscribe(UUID userId, UUID interestId) {
        Interest interest = getInterest(interestId);

        subscriptionRepository.findByUser_IdAndInterest_Id(userId, interestId)
                .ifPresent(subscription -> {
                    subscriptionRepository.delete(subscription);
                    interest.decreaseSubscriberCount();
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
