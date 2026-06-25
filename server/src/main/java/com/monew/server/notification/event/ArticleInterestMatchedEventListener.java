package com.monew.server.notification.event;

import com.monew.server.interest.event.ArticleInterestMatchedEvent;
import com.monew.server.interest.repository.SubscriptionRepository;
import com.monew.server.notification.service.NotificationService;
import java.util.HashSet;
import java.util.Set;
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
        if (event.interestIds().isEmpty()) {
            return;
        }

        Set<UUID> subscriberIds = new HashSet<>();

        for (UUID interestId : event.interestIds()) {
            subscriberIds.addAll(subscriptionRepository.findUserIdsByInterestId(interestId));
        }

        UUID resourceInterestId = event.interestIds().get(0);

        for (UUID subscriberId : subscriberIds) {
            notificationService.createInterestNewsNotification(
                    subscriberId,
                    resourceInterestId,
                    event.articleId(),
                    event.articleTitle()
            );
        }
    }
}