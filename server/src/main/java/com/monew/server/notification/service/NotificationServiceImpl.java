package com.monew.server.notification.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.notification.NotificationErrorCode;
import com.monew.server.common.exception.notification.NotificationException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.notification.dto.NotificationResponse;
import com.monew.server.notification.entity.Notification;
import com.monew.server.notification.entity.NotificationResourceType;
import com.monew.server.notification.repository.NotificationRepository;
import com.monew.server.notification.repository.querydsl.NotificationQueryRepository;
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
        validateUserExists(userId);
        validateLimit(limit);
        validateCursorPair(cursor, after);

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
        validateUserExists(userId);
        validateRequired("notificationId", notificationId);

        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> {
                    NotificationException exception =
                            new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
                    exception.addDetail("notificationId", notificationId);
                    exception.addDetail("userId", userId);
                    return exception;
                });

        notification.confirm();
    }

    @Override
    @Transactional
    public void confirmAll(UUID userId) {
        validateUserExists(userId);

        notificationRepository.confirmAllByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void createCommentLikeNotification(
            UUID receiverUserId,
            UUID commentId,
            String likerNickname
    ) {
        validateRequired("receiverUserId", receiverUserId);
        validateRequired("commentId", commentId);
        validateText("likerNickname", likerNickname);

        String content = likerNickname + "님이 나의 댓글을 좋아합니다.";

        createNotification(
                receiverUserId,
                content,
                NotificationResourceType.COMMENT,
                commentId
        );
    }

    private void createNotification(
            UUID receiverUserId,
            String content,
            NotificationResourceType resourceType,
            UUID resourceId
    ) {
        validateRequired("receiverUserId", receiverUserId);
        validateText("content", content);
        validateRequired("resourceType", resourceType);
        validateRequired("resourceId", resourceId);

        User receiver = userRepository.findById(receiverUserId)
                .orElseThrow(() -> {
                    BaseException exception = new BaseException(UserErrorCode.USER_NOT_FOUND);
                    exception.addDetail("receiverUserId", receiverUserId);
                    return exception;
                });

        Notification notification = Notification.builder()
                .user(receiver)
                .content(content)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();

        notificationRepository.save(notification);
    }

    private void validateUserExists(UUID userId) {
        validateRequired("userId", userId);

        if (!userRepository.existsById(userId)) {
            BaseException exception = new BaseException(UserErrorCode.USER_NOT_FOUND);
            exception.addDetail("userId", userId);
            throw exception;
        }
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            NotificationException exception =
                    new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);
            exception.addDetail("limit", limit);
            throw exception;
        }
    }

    private void validateCursorPair(String cursor, LocalDateTime after) {
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasAfter = after != null;

        if (hasCursor != hasAfter) {
            NotificationException exception =
                    new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_CURSOR);
            exception.addDetail("cursor", cursor);
            exception.addDetail("after", after);
            throw exception;
        }
    }

    private void validateRequired(String fieldName, Object value) {
        if (value == null) {
            NotificationException exception =
                    new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);
            exception.addDetail(fieldName, null);
            throw exception;
        }
    }

    private void validateText(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            NotificationException exception =
                    new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);
            exception.addDetail(fieldName, value);
            throw exception;
        }
    }
}