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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterestMatchedArticleEventListener {

    private static final int NOTIFICATION_CHUNK_SIZE = 500;

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    @EventListener
    @Transactional
    public void handle(InterestMatchedArticleEvent event) {
        if (event == null || event.interests() == null || event.interests().isEmpty()) {
            return;
        }

        Map<UUID, Set<UUID>> articleIdsByInterestId = new LinkedHashMap<>();

        for (InterestMatchedArticleEvent.InterestMatchData match : event.interests()) {
            if (match == null || match.articleId() == null || match.interestId() == null) {
                continue;
            }

            articleIdsByInterestId
                    .computeIfAbsent(match.interestId(), ignored -> new LinkedHashSet<>())
                    .add(match.articleId());
        }

        Map<UUID, String> interestNamesByInterestId = event.interests().stream()
                .filter(match -> match != null
                        && match.articleId() != null
                        && match.interestId() != null)
                .collect(Collectors.toMap(
                        InterestMatchedArticleEvent.InterestMatchData::interestId,
                        InterestMatchedArticleEvent.InterestMatchData::interestName,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        for (Map.Entry<UUID, Set<UUID>> entry : articleIdsByInterestId.entrySet()) {
            UUID interestId = entry.getKey();
            int articleCount = entry.getValue().size();

            if (articleCount <= 0) {
                continue;
            }

            String content = createContent(interestNamesByInterestId.get(interestId), articleCount);
            saveNotificationsByChunk(interestId, content);
        }
    }

    private void saveNotificationsByChunk(UUID interestId, String content) {
        int page = 0;

        while (true) {
            List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestId(
                    interestId,
                    PageRequest.of(page, NOTIFICATION_CHUNK_SIZE)
            );

            if (subscriberIds.isEmpty()) {
                break;
            }

            List<Notification> notifications = new ArrayList<>(subscriberIds.size());

            for (UUID subscriberId : subscriberIds) {
                User userReference = entityManager.getReference(User.class, subscriberId);

                notifications.add(Notification.builder()
                        .user(userReference)
                        .content(content)
                        .resourceType(NotificationResourceType.INTEREST)
                        .resourceId(interestId)
                        .build());
            }

            notificationRepository.saveAll(notifications);

            if (subscriberIds.size() < NOTIFICATION_CHUNK_SIZE) {
                break;
            }

            page++;
        }
    }

    private String createContent(String interestName, int articleCount) {
        return interestName + "와 관련된 기사가 " + articleCount + "건 등록되었습니다.";
    }
}