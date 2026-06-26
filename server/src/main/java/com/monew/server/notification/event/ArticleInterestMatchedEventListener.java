package com.monew.server.notification.event;

import com.monew.server.interest.event.ArticleInterestMatchedEvent;
import com.monew.server.interest.repository.SubscriptionRepository;
import com.monew.server.notification.service.NotificationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArticleInterestMatchedEventListener {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ArticleInterestMatchedEvent event) {
        if (event == null || event.interestIds() == null || event.interestIds().isEmpty()) {
            return;
        }

        Map<UUID, UUID> subscriberInterestMap = new LinkedHashMap<>();

        for (UUID interestId : event.interestIds()) {
            if (interestId == null) {
                continue;
            }

            List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestId(interestId);

            if (subscriberIds == null || subscriberIds.isEmpty()) {
                continue;
            }

            for (UUID subscriberId : subscriberIds) {
                if (subscriberId == null) {
                    continue;
                }

                subscriberInterestMap.putIfAbsent(subscriberId, interestId);
            }
        }

        for (Map.Entry<UUID, UUID> entry : subscriberInterestMap.entrySet()) {
            notificationService.createInterestNewsNotification(
                    entry.getKey(),
                    entry.getValue(),
                    event.articleId(),
                    event.articleTitle()
            );
        }
    }
}