package com.monew.server.activity.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.document.UserActivity.ArticleViewActivity;
import com.monew.server.activity.document.UserActivity.CommentActivity;
import com.monew.server.activity.document.UserActivity.CommentLikeActivity;
import com.monew.server.activity.document.UserActivity.SubscriptionActivity;
import com.monew.server.activity.service.UserActivityUpdater;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UserActivityEventHandlerTest {

  private final UserActivityUpdater userActivityUpdater =
      Mockito.mock(UserActivityUpdater.class);

  private final UserActivityEventHandler handler =
      new UserActivityEventHandler(userActivityUpdater);

  @Test
  void 회원가입_이벤트를_받으면_활동문서를_생성한다() {
    UUID userId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.now();

    UserCreatedEvent event = new UserCreatedEvent(
        userId,
        "test@monew.com",
        "테스터",
        createdAt
    );

    handler.handle(event);

    ArgumentCaptor<UserActivity> captor =
        ArgumentCaptor.forClass(UserActivity.class);

    verify(userActivityUpdater).create(captor.capture());

    UserActivity saved = captor.getValue();

    assertThat(saved.getId()).isEqualTo(userId.toString());
    assertThat(saved.getEmail()).isEqualTo("test@monew.com");
    assertThat(saved.getNickname()).isEqualTo("테스터");
    assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  void 닉네임수정_이벤트를_받으면_닉네임을_수정한다() {
    UUID userId = UUID.randomUUID();

    handler.handle(new UserNicknameUpdatedEvent(userId, "변경닉네임"));

    verify(userActivityUpdater).updateNickname(
        userId.toString(),
        "변경닉네임"
    );
  }

  @Test
  void 사용자삭제_이벤트를_받으면_활동문서를_삭제한다() {
    UUID userId = UUID.randomUUID();

    handler.handle(new UserDeletedEvent(userId));

    verify(userActivityUpdater).deleteUser(userId.toString());
  }

  @Test
  void 구독생성_이벤트를_받으면_구독목록에_추가한다() {
    UUID userId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
        userId,
        subscriptionId,
        interestId,
        "경제",
        List.of("금리", "환율"),
        10L,
        LocalDateTime.now()
    );

    handler.handle(event);

    verify(userActivityUpdater).addSubscription(
        eq(userId.toString()),
        any(SubscriptionActivity.class)
    );
  }

  @Test
  void 구독삭제_이벤트를_받으면_구독목록에서_제거한다() {
    UUID userId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();

    handler.handle(new SubscriptionDeletedEvent(userId, subscriptionId));

    verify(userActivityUpdater).removeSubscription(
        userId.toString(),
        subscriptionId.toString()
    );
  }

  @Test
  void 댓글작성_이벤트를_받으면_댓글목록에_추가한다() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    CommentCreatedEvent event = new CommentCreatedEvent(
        userId,
        commentId,
        articleId,
        "기사 제목",
        "작성자",
        "댓글 내용",
        0L,
        LocalDateTime.now()
    );

    handler.handle(event);

    verify(userActivityUpdater).addComment(
        eq(userId.toString()),
        any(CommentActivity.class)
    );
  }

  @Test
  void 댓글수정_이벤트를_받으면_작성댓글과_좋아요한댓글_내용을_수정한다() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    handler.handle(new CommentUpdatedEvent(userId, commentId, "수정된 댓글"));

    verify(userActivityUpdater).updateComment(
        userId.toString(),
        commentId.toString(),
        "수정된 댓글"
    );

    verify(userActivityUpdater).updateLikeCommentContent(
        commentId.toString(),
        "수정된 댓글"
    );
  }

  @Test
  void 댓글삭제_이벤트를_받으면_작성댓글과_좋아요한댓글목록에서_제거한다() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    handler.handle(new CommentDeletedEvent(userId, commentId));

    verify(userActivityUpdater).deleteComment(
        userId.toString(),
        commentId.toString()
    );

    verify(userActivityUpdater).removeDeletedCommentLikes(
        commentId.toString()
    );
  }

  @Test
  void 댓글좋아요_이벤트를_받으면_좋아요목록에_추가하고_좋아요수를_갱신한다() {
    UUID userId = UUID.randomUUID();
    UUID commentLikeId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID commentUserId = UUID.randomUUID();

    CommentLikeCreatedEvent event = new CommentLikeCreatedEvent(
        userId,
        commentLikeId,
        LocalDateTime.now(),
        commentId,
        articleId,
        "기사 제목",
        commentUserId,
        "댓글작성자",
        "댓글 내용",
        1L,
        LocalDateTime.now()
    );

    handler.handle(event);

    verify(userActivityUpdater).addCommentLike(
        eq(userId.toString()),
        any(CommentLikeActivity.class)
    );

    verify(userActivityUpdater).updateCommentLikeCount(
        commentId.toString(),
        1L
    );
  }

  @Test
  void 댓글좋아요취소_이벤트를_받으면_좋아요목록에서_제거하고_좋아요수를_갱신한다() {
    UUID userId = UUID.randomUUID();
    UUID commentLikeId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    CommentLikeDeletedEvent event = new CommentLikeDeletedEvent(
        userId,
        commentLikeId,
        commentId,
        0L
    );

    handler.handle(event);

    verify(userActivityUpdater).removeCommentLike(
        userId.toString(),
        commentLikeId.toString()
    );

    verify(userActivityUpdater).updateCommentLikeCount(
        commentId.toString(),
        0L
    );
  }

  @Test
  void 기사조회_이벤트를_받으면_최근조회기사에_추가한다() {
    UUID userId = UUID.randomUUID();
    UUID articleViewId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();

    ArticleViewedEvent event = new ArticleViewedEvent(
        userId,
        articleViewId,
        articleId,
        "NAVER",
        "https://news.example.com/1",
        "기사 제목",
        LocalDateTime.now(),
        "기사 요약",
        3L,
        10L,
        LocalDateTime.now()
    );

    handler.handle(event);

    verify(userActivityUpdater).addArticleView(
        eq(userId.toString()),
        any(ArticleViewActivity.class)
    );
  }

  @Test
  void 기사댓글수변경_이벤트를_받으면_최근조회기사의_댓글수를_갱신한다() {
    UUID articleId = UUID.randomUUID();

    handler.handle(new ArticleCommentCountUpdatedEvent(articleId, 5L));

    verify(userActivityUpdater).updateArticleViewCommentCount(
        articleId.toString(),
        5L
    );
  }

  @Test
  void 기사조회수변경_이벤트를_받으면_최근조회기사의_조회수를_갱신한다() {
    UUID articleId = UUID.randomUUID();

    handler.handle(new ArticleViewCountUpdatedEvent(articleId, 20L));

    verify(userActivityUpdater).updateArticleViewCount(
        articleId.toString(),
        20L
    );
  }

  @Test
  void 기사삭제_이벤트를_받으면_최근조회기사에서_제거한다() {
    UUID articleId = UUID.randomUUID();

    handler.handle(new ArticleDeletedEvent(articleId));

    verify(userActivityUpdater).removeArticleViews(articleId.toString());
  }
}