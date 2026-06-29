package com.monew.server.activity.event;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.document.UserActivity.ArticleViewActivity;
import com.monew.server.activity.document.UserActivity.CommentActivity;
import com.monew.server.activity.document.UserActivity.CommentLikeActivity;
import com.monew.server.activity.document.UserActivity.SubscriptionActivity;
import com.monew.server.activity.service.UserActivityUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserActivityEventHandler {

  private final UserActivityUpdater userActivityUpdater;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserCreatedEvent event){
    UserActivity userActivity = UserActivity.create(
        event.userId().toString(),
        event.email(),
        event.nickname(),
        event.createdAt()
    );
    userActivityUpdater.create(userActivity);
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserNicknameUpdatedEvent event) {
    userActivityUpdater.updateNickname(
        event.userId().toString(),
        event.nickname()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserDeletedEvent event) {
    userActivityUpdater.deleteUser(event.userId().toString());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCreatedEvent event) {
    SubscriptionActivity subscription = new SubscriptionActivity(
        event.subscriptionId().toString(),
        event.interestId().toString(),
        event.interestName(),
        event.interestKeywords(),
        event.interestSubscriberCount(),
        event.createdAt()
    );

    userActivityUpdater.addSubscription(
        event.userId().toString(),
        subscription
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionDeletedEvent event) {
    userActivityUpdater.removeSubscription(
        event.userId().toString(),
        event.subscriptionId().toString()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentCreatedEvent event) {
    CommentActivity comment = new CommentActivity(
        event.commentId().toString(),
        event.articleId().toString(),
        event.articleTitle(),
        event.userId().toString(),
        event.userNickname(),
        event.content(),
        event.likeCount(),
        event.createdAt()
    );

    userActivityUpdater.addComment(
        event.userId().toString(),
        comment
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentUpdatedEvent event) {
    userActivityUpdater.updateComment(
        event.userId().toString(),
        event.commentId().toString(),
        event.content()
    );

    userActivityUpdater.updateLikeCommentContent(
        event.commentId().toString(),
        event.content()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentDeletedEvent event) {
    userActivityUpdater.deleteComment(
        event.userId().toString(),
        event.commentId().toString()
    );

    userActivityUpdater.removeDeletedCommentLikes(
        event.commentId().toString()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikeCreatedEvent event) {
    CommentLikeActivity commentLike = new CommentLikeActivity(
        event.commentLikeId().toString(),
        event.createdAt(),
        event.commentId().toString(),
        event.articleId().toString(),
        event.articleTitle(),
        event.commentUserId().toString(),
        event.commentUserNickname(),
        event.commentContent(),
        event.commentLikeCount(),
        event.commentCreatedAt()
    );

    userActivityUpdater.addCommentLike(
        event.userId().toString(),
        commentLike
    );

    userActivityUpdater.updateCommentLikeCount(
        event.commentId().toString(),
        event.commentLikeCount()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikeDeletedEvent event) {
    userActivityUpdater.removeCommentLike(
        event.userId().toString(),
        event.commentLikeId().toString()
    );

    userActivityUpdater.updateCommentLikeCount(
        event.commentId().toString(),
        event.commentLikeCount()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleViewedEvent event) {
    ArticleViewActivity articleView = new ArticleViewActivity(
        event.articleViewId().toString(),
        event.userId().toString(),
        event.createdAt(),
        event.articleId().toString(),
        event.source(),
        event.sourceUrl(),
        event.articleTitle(),
        event.articlePublishedDate(),
        event.articleSummary(),
        event.articleCommentCount(),
        event.articleViewCount()
    );

    userActivityUpdater.addArticleView(
        event.userId().toString(),
        articleView
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleCommentCountUpdatedEvent event) {
    userActivityUpdater.updateArticleViewCommentCount(
        event.articleId().toString(),
        event.articleCommentCount()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleViewCountUpdatedEvent event) {
    userActivityUpdater.updateArticleViewCount(
        event.articleId().toString(),
        event.articleViewCount()
    );
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleDeletedEvent event) {
    userActivityUpdater.removeArticleViews(
        event.articleId().toString()
    );
  }
}
