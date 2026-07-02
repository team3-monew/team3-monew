package com.monew.batch.notification.event;

import com.monew.batch.article.event.InterestMatchedArticleEvent;
import com.monew.batch.notification.entity.Notification;
import com.monew.batch.notification.entity.NotificationResourceType;
import com.monew.batch.notification.repository.NotificationRepository;
import com.monew.batch.subscription.repository.SubscriptionRepository;
import com.monew.batch.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterestMatchedArticleEventListener {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    @EventListener
    @Transactional
    public void handle(InterestMatchedArticleEvent event) {
        if (event == null || event.interests() == null || event.interests().isEmpty()) {
            return;
        }

        Map<UUID, Long> articleCountsByInterestId = event.interests().stream()
                .filter(interest -> interest != null && interest.interestId() != null)
                .collect(Collectors.groupingBy(
                        InterestMatchedArticleEvent.InterestMatchData::interestId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<UUID, String> interestNamesByInterestId = event.interests().stream()
                .filter(interest -> interest != null && interest.interestId() != null)
                .collect(Collectors.toMap(
                        InterestMatchedArticleEvent.InterestMatchData::interestId,
                        InterestMatchedArticleEvent.InterestMatchData::interestName,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<Notification> notifications = new ArrayList<>();

        for (Map.Entry<UUID, Long> entry : articleCountsByInterestId.entrySet()) {
            UUID interestId = entry.getKey();
            int articleCount = entry.getValue().intValue();

            if (articleCount <= 0) {
                continue;
            }

            List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestId(interestId);

            for (UUID subscriberId : subscriberIds) {
                User userReference = entityManager.getReference(User.class, subscriberId);

                notifications.add(Notification.builder()
                        .user(userReference)
                        .content(createContent(interestNamesByInterestId.get(interestId), articleCount))
                        .resourceType(NotificationResourceType.INTEREST)
                        .resourceId(interestId)
                        .build());
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    private String createContent(String interestName, int articleCount) {
        return interestName + "와 관련된 기사가 " + articleCount + "건 등록되었습니다.";
    }
}