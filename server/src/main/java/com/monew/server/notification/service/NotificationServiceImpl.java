package com.monew.server.notification.service;

import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.notification.dto.NotificationResponse;
import com.monew.server.notification.entity.Notification;
import com.monew.server.notification.entity.NotificationResourceType;
import com.monew.server.notification.repository.NotificationQueryRepository;
import com.monew.server.notification.repository.NotificationRepository;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationQueryRepository notificationQueryRepository;
    private final UserRepository userRepository;

    @Override
    public CursorPageResponse<NotificationResponse> findUnreadNotifications(
            UUID userId,
            String cursor,
            LocalDateTime after,
            int limit
    ) {
        validateLimit(limit);

        List<NotificationResponse> queriedNotifications =
                notificationQueryRepository.findUnreadNotifications(
                        userId,
                        cursor,
                        after,
                        limit
                );

        boolean hasNext = queriedNotifications.size() > limit;

        List<NotificationResponse> content = hasNext
                ? queriedNotifications.subList(0, limit)
                : queriedNotifications;

        String nextCursor = null;
        LocalDateTime nextAfter = null;

        if (hasNext && !content.isEmpty()) {
            NotificationResponse lastNotification = content.get(content.size() - 1);
            nextCursor = lastNotification.id().toString();
            nextAfter = lastNotification.createdAt();
        }

        long totalElements = notificationQueryRepository.countUnreadNotifications(userId);

        return new CursorPageResponse<>(
                content,
                nextCursor,
                nextAfter,
                content.size(),
                totalElements,
                hasNext
        );
    }

    @Override
    @Transactional
    public void confirm(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        notification.confirm();
    }

    @Override
    @Transactional
    public void confirmAll(UUID userId) {
        notificationRepository.confirmAllByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void createCommentLikeNotification(
            UUID receiverUserId,
            UUID commentId,
            String likerNickname
    ) {
        String content = likerNickname + "님이 회원님의 댓글을 좋아합니다.";

        createNotification(
                receiverUserId,
                content,
                NotificationResourceType.COMMENT,
                commentId
        );
    }

    @Override
    @Transactional
    public void createInterestNewsNotification(
            UUID receiverUserId,
            UUID interestId,
            UUID articleId,
            String articleTitle
    ) {
        String content = "관심사와 관련된 새 기사가 등록되었습니다: " + articleTitle;

        createNotification(
                receiverUserId,
                content,
                NotificationResourceType.INTEREST,
                interestId
        );
    }

    private void createNotification(
            UUID receiverUserId,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId
    ) {
        User receiver = userRepository.findById(receiverUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = Notification.builder()
                .user(receiver)
                .content(content)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();

        notificationRepository.save(notification);
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit은 1 이상이어야 합니다.");
        }
    }
}