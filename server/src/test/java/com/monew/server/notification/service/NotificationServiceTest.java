package com.monew.server.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.notification.NotificationErrorCode;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final int MAX_LIMIT = 50;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    NotificationQueryRepository notificationQueryRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    NotificationServiceImpl notificationService;

    @Test
    @DisplayName("미확인 알림 조회 성공 - limit 초과 결과가 있으면 다음 커서와 hasNext를 생성한다")
    void findUnreadNotifications_success_hasNext() {
        // given
        UUID userId = UUID.randomUUID();
        int limit = 2;
        NotificationResponse first = response(UUID.randomUUID(), userId, LocalDateTime.of(2026, 7, 1, 10, 0));
        NotificationResponse second = response(UUID.randomUUID(), userId, LocalDateTime.of(2026, 7, 1, 9, 0));
        NotificationResponse extra = response(UUID.randomUUID(), userId, LocalDateTime.of(2026, 7, 1, 8, 0));

        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);
        given(notificationQueryRepository.findUnreadNotifications(userId, null, null, limit))
                .willReturn(List.of(first, second, extra));
        given(notificationQueryRepository.countUnreadNotifications(userId)).willReturn(3L);

        // when
        CursorPageResponse<NotificationResponse> result =
                notificationService.findUnreadNotifications(userId, null, null, limit);

        // then
        assertThat(result.content()).containsExactly(first, second);
        assertThat(result.nextCursor()).isEqualTo(second.id().toString());
        assertThat(result.nextAfter()).isEqualTo(second.createdAt());
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3L);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("미확인 알림 조회 성공 - 마지막 페이지이면 다음 커서를 생성하지 않는다")
    void findUnreadNotifications_success_lastPage() {
        // given
        UUID userId = UUID.randomUUID();
        int limit = 10;
        NotificationResponse notification = response(UUID.randomUUID(), userId, LocalDateTime.of(2026, 7, 1, 10, 0));

        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);
        given(notificationQueryRepository.findUnreadNotifications(userId, null, null, limit))
                .willReturn(List.of(notification));
        given(notificationQueryRepository.countUnreadNotifications(userId)).willReturn(1L);

        // when
        CursorPageResponse<NotificationResponse> result =
                notificationService.findUnreadNotifications(userId, null, null, limit);

        // then
        assertThat(result.content()).containsExactly(notification);
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextAfter()).isNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("미확인 알림 조회 실패 - userId가 null이면 잘못된 요청 예외가 발생한다")
    void findUnreadNotifications_fail_nullUserId() {
        // given
        UUID userId = null;

        // when
        // then
        assertThatThrownBy(() -> notificationService.findUnreadNotifications(userId, null, null, 10))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);

        then(userRepository).should(never()).existsByIdAndDeletedAtIsNull(any());
        then(notificationQueryRepository).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("미확인 알림 조회 실패 - 사용자가 없으면 사용자 없음 예외가 발생한다")
    void findUnreadNotifications_fail_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(false);

        // when
        // then
        assertThatThrownBy(() -> notificationService.findUnreadNotifications(userId, null, null, 10))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        then(notificationQueryRepository).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("미확인 알림 조회 실패 - limit이 1 미만이면 잘못된 요청 예외가 발생한다")
    void findUnreadNotifications_fail_invalidLimit_underOne() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> notificationService.findUnreadNotifications(userId, null, null, 0))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);

        then(notificationQueryRepository).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("미확인 알림 조회 실패 - limit이 최대값을 초과하면 잘못된 요청 예외가 발생한다")
    void findUnreadNotifications_fail_invalidLimit_overMax() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> notificationService.findUnreadNotifications(userId, null, null, MAX_LIMIT + 1))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);

        then(notificationQueryRepository).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("미확인 알림 조회 실패 - cursor만 있으면 커서 예외가 발생한다")
    void findUnreadNotifications_fail_onlyCursor() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> notificationService.findUnreadNotifications(userId, UUID.randomUUID().toString(), null, 10))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_CURSOR);

        then(notificationQueryRepository).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("미확인 알림 조회 실패 - after만 있으면 커서 예외가 발생한다")
    void findUnreadNotifications_fail_onlyAfter() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> notificationService.findUnreadNotifications(userId, null, LocalDateTime.now(), 10))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_CURSOR);

        then(notificationQueryRepository).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("알림 단건 확인 성공 - 알림을 조회한 뒤 확인 상태로 변경한다")
    void confirm_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = notification(user(userId), UUID.randomUUID());

        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);
        given(notificationRepository.findByIdAndUserId(notificationId, userId)).willReturn(Optional.of(notification));

        // when
        notificationService.confirm(userId, notificationId);

        // then
        assertThat(notification.isConfirmed()).isTrue();
        assertThat(notification.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("알림 단건 확인 실패 - notificationId가 null이면 잘못된 요청 예외가 발생한다")
    void confirm_fail_nullNotificationId() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        // then
        assertThatThrownBy(() -> notificationService.confirm(userId, null))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);

        then(notificationRepository).should(never()).findByIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("알림 단건 확인 실패 - 알림이 없으면 알림 없음 예외가 발생한다")
    void confirm_fail_notificationNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);
        given(notificationRepository.findByIdAndUserId(notificationId, userId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> notificationService.confirm(userId, notificationId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("알림 전체 확인 성공 - 사용자 존재 확인 후 전체 확인 벌크 쿼리를 실행한다")
    void confirmAll_success() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        notificationService.confirmAll(userId);

        // then
        then(notificationRepository).should().confirmAllByUserId(eq(userId), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("댓글 좋아요 알림 생성 성공 - 수신자에게 댓글 리소스 알림을 저장한다")
    void createCommentLikeNotification_success() {
        // given
        UUID receiverUserId = UUID.randomUUID();
        UUID likerUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        User receiver = user(receiverUserId);

        given(userRepository.findByIdAndDeletedAtIsNull(receiverUserId)).willReturn(Optional.of(receiver));

        // when
        notificationService.createCommentLikeNotification(receiverUserId, likerUserId, commentId, "좋아요유저");

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        then(notificationRepository).should().save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(receiver);
        assertThat(saved.getContent()).isEqualTo("좋아요유저님이 나의 댓글을 좋아합니다.");
        assertThat(saved.getResourceType()).isEqualTo(NotificationResourceType.COMMENT);
        assertThat(saved.getResourceId()).isEqualTo(commentId);
        assertThat(saved.isConfirmed()).isFalse();
    }

    @Test
    @DisplayName("댓글 좋아요 알림 생성 성공 - 본인이 본인 댓글에 좋아요를 누르면 알림 생성을 생략한다")
    void createCommentLikeNotification_success_selfLikeSkip() {
        // given
        UUID userId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        // when
        notificationService.createCommentLikeNotification(userId, userId, commentId, "본인");

        // then
        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
        then(notificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("댓글 좋아요 알림 생성 실패 - 수신자가 없으면 사용자 없음 예외가 발생한다")
    void createCommentLikeNotification_fail_receiverNotFound() {
        // given
        UUID receiverUserId = UUID.randomUUID();
        UUID likerUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        given(userRepository.findByIdAndDeletedAtIsNull(receiverUserId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> notificationService.createCommentLikeNotification(receiverUserId, likerUserId, commentId, "좋아요유저"))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        then(notificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("댓글 좋아요 알림 생성 실패 - likerNickname이 공백이면 잘못된 요청 예외가 발생한다")
    void createCommentLikeNotification_fail_blankNickname() {
        // given
        UUID receiverUserId = UUID.randomUUID();
        UUID likerUserId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        // when
        // then
        assertThatThrownBy(() -> notificationService.createCommentLikeNotification(receiverUserId, likerUserId, commentId, "  "))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);

        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
        then(notificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("댓글 좋아요 알림 생성 실패 - commentId가 null이면 잘못된 요청 예외가 발생한다")
    void createCommentLikeNotification_fail_nullCommentId() {
        // given
        UUID receiverUserId = UUID.randomUUID();
        UUID likerUserId = UUID.randomUUID();

        // when
        // then
        assertThatThrownBy(() -> notificationService.createCommentLikeNotification(receiverUserId, likerUserId, null, "좋아요유저"))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(NotificationErrorCode.INVALID_NOTIFICATION_REQUEST);

        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
        then(notificationRepository).should(never()).save(any());
    }

    private NotificationResponse response(UUID notificationId, UUID userId, LocalDateTime createdAt) {
        return new NotificationResponse(
                notificationId,
                createdAt,
                null,
                false,
                userId,
                "알림 내용",
                "comment",
                UUID.randomUUID()
        );
    }

    private Notification notification(User user, UUID resourceId) {
        return Notification.builder()
                .user(user)
                .content("알림 내용")
                .resourceType(NotificationResourceType.COMMENT)
                .resourceId(resourceId)
                .build();
    }

    private User user(UUID userId) {
        User user = new User("user" + userId + "@example.com", "사용자", "Password1!");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}