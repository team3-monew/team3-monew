package com.monew.server.activity.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.document.UserActivity.ArticleViewActivity;
import com.monew.server.activity.document.UserActivity.CommentActivity;
import com.monew.server.activity.document.UserActivity.CommentLikeActivity;
import com.monew.server.activity.document.UserActivity.SubscriptionActivity;
import com.monew.server.activity.repository.UserActivityRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class UserActivityUpdaterTest {

  private final UserActivityRepository userActivityRepository =
      Mockito.mock(UserActivityRepository.class);

  private final MongoTemplate mongoTemplate =
      Mockito.mock(MongoTemplate.class);

  private final UserActivityUpdater userActivityUpdater =
      new UserActivityUpdater(userActivityRepository, mongoTemplate);

  @Test
  void 활동문서를_생성한다() {
    UserActivity activity = UserActivity.create(
        "user-id",
        "test@monew.com",
        "테스터",
        LocalDateTime.now()
    );

    userActivityUpdater.create(activity);

    verify(userActivityRepository).save(activity);
  }

  @Test
  void 닉네임을_수정한다() {
    userActivityUpdater.updateNickname("user-id", "새닉네임");

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 사용자_활동문서를_삭제한다() {
    userActivityUpdater.deleteUser("user-id");

    verify(userActivityRepository).deleteById("user-id");
  }

  @Test
  void 구독을_추가한다() {
    SubscriptionActivity subscription = new SubscriptionActivity(
        "subscription-id",
        "interest-id",
        "경제",
        List.of("금리", "환율"),
        10L,
        LocalDateTime.now()
    );

    userActivityUpdater.addSubscription("user-id", subscription);

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 구독을_삭제한다() {
    userActivityUpdater.removeSubscription("user-id", "subscription-id");

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 댓글을_추가한다() {
    CommentActivity comment = new CommentActivity(
        "comment-id",
        "article-id",
        "기사 제목",
        "user-id",
        "작성자",
        "댓글 내용",
        0L,
        LocalDateTime.now()
    );

    userActivityUpdater.addComment("user-id", comment);

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 댓글내용을_수정한다() {
    userActivityUpdater.updateComment(
        "user-id",
        "comment-id",
        "수정된 댓글"
    );

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 댓글을_삭제한다() {
    userActivityUpdater.deleteComment("user-id", "comment-id");

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 댓글좋아요를_추가한다() {
    CommentLikeActivity commentLike = new CommentLikeActivity(
        "comment-like-id",
        LocalDateTime.now(),
        "comment-id",
        "article-id",
        "기사 제목",
        "comment-user-id",
        "댓글작성자",
        "댓글 내용",
        1L,
        LocalDateTime.now()
    );

    userActivityUpdater.addCommentLike("user-id", commentLike);

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 댓글좋아요를_삭제한다() {
    userActivityUpdater.removeCommentLike("user-id", "comment-like-id");

    verify(mongoTemplate).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 댓글좋아요수를_갱신한다() {
    userActivityUpdater.updateCommentLikeCount("comment-id", 3L);

    verify(mongoTemplate, times(2)).updateMulti(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 좋아요한댓글_내용을_수정한다() {
    userActivityUpdater.updateLikeCommentContent(
        "comment-id",
        "수정된 댓글"
    );

    verify(mongoTemplate).updateMulti(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 삭제된댓글을_좋아요목록에서_제거한다() {
    userActivityUpdater.removeDeletedCommentLikes("comment-id");

    verify(mongoTemplate).updateMulti(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 기사조회기록을_추가한다() {
    ArticleViewActivity articleView = new ArticleViewActivity(
        "article-view-id",
        "user-id",
        LocalDateTime.now(),
        "article-id",
        "NAVER",
        "https://news.example.com/1",
        "기사 제목",
        LocalDateTime.now(),
        "기사 요약",
        2L,
        10L
    );

    userActivityUpdater.addArticleView("user-id", articleView);

    verify(mongoTemplate, times(2)).updateFirst(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 기사댓글수를_갱신한다() {
    userActivityUpdater.updateArticleViewCommentCount("article-id", 5L);

    verify(mongoTemplate).updateMulti(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 기사조회수를_갱신한다() {
    userActivityUpdater.updateArticleViewCount("article-id", 20L);

    verify(mongoTemplate).updateMulti(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }

  @Test
  void 삭제된기사를_최근조회기사에서_제거한다() {
    userActivityUpdater.removeArticleViews("article-id");

    verify(mongoTemplate).updateMulti(
        any(Query.class),
        any(Update.class),
        eq(UserActivity.class)
    );
  }
}