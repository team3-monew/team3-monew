package com.monew.server.subscription.service;

import com.monew.server.subscription.dto.SubscriptionDto;

import java.util.UUID;

public interface SubscriptionService {

    // 구독
    SubscriptionDto subscribe(UUID userId, UUID interestId);

    //구독 취소
    void unsubscribe(UUID userId, UUID interestId);
}
