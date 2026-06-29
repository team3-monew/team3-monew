package com.monew.server.activity.service;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.document.UserActivity.ArticleViewActivity;
import com.monew.server.activity.document.UserActivity.CommentActivity;
import com.monew.server.activity.document.UserActivity.CommentLikeActivity;
import com.monew.server.activity.document.UserActivity.SubscriptionActivity;
import com.monew.server.activity.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActivityUpdater {

  private final UserActivityRepository userActivityRepository;
  private final MongoTemplate mongoTemplate;

  private static final int RECENT_ACTIVITY_LIMIT = 10;

  // ==================== 사용자 ====================
  // 회원가입 시 사용자 활동 문서를 최초 생성합니다
  public void create(UserActivity userActivity) {
    userActivityRepository.save(userActivity);
  }

  // 사용자 닉네임 변경
  public void updateNickname(String userId, String nickname) {
    Query query = findUser(userId);

    Update update = new Update().set("nickname", nickname);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 사용자 논리 삭제 시 활동 문서 제거
  public void deleteUser(String userId) {
    userActivityRepository.deleteById(userId);
  }

  // ==================== 관심사 구독 ====================
  // 현재 구독 목록에 관심사를 추가
  public void addSubscription(
      String userId, SubscriptionActivity subscription
  ) {
    Query query = Query.query(
        Criteria.where("_id").is(userId)
            .and("subscriptions.id").ne(subscription.getId())
    );

    Update update = new Update()
        .push("subscriptions", subscription);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 구독 취소 시 해당 구독을 제거
  public void removeSubscription(String userId, String subscriptionId) {
    Query query = findUser(userId);

    Update update = new Update()
        .pull("subscriptions", new Document("id", subscriptionId)
        );

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // ==================== 최근 작성 댓글 ====================
  // 새 댓글을 맨 앞에 추가하고 최근 10건만 유지
  public void addComment(String userId, CommentActivity comment) {
    Query query = findUser(userId);

    Update update = new Update()
        .push("comments")
        .atPosition(0)
        .slice(RECENT_ACTIVITY_LIMIT)
        .each(comment);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 작성한 댓글의 내용을 수정
  public void updateComment(
      String userId, String commentId, String content) {
    Query query = Query.query(
        Criteria.where("_id").is(userId)
            .and("comments.id").is(commentId)
    );

    Update update = new Update()
        .set("comments.$.content", content);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 삭제된 댓글을 리스트에서 제거
  public void deleteComment(String userId, String commentId) {
    Query query = findUser(userId);

    Update update = new Update()
        .pull("comments", new Document("id", commentId));

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // ==================== 최근 좋아요 댓글 ====================
  // 좋아요한 댓글을 맨 앞에 추가하고 최근 10건만 유지
  public void addCommentLike(String userId, CommentLikeActivity commentLike) {
    Query query = Query.query(
        Criteria.where("_id").is(userId)
            .and("commentLikes.id").ne(commentLike.getId())
    );

    Update update = new Update()
        .push("commentLikes")
        .atPosition(0)
        .slice(RECENT_ACTIVITY_LIMIT)
        .each(commentLike);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 좋아요 취소 시 좋아요 활동을 제거
  public void removeCommentLike(String userId, String commentLikeId) {
    Query query = findUser(userId);

    Update update = new Update()
        .pull("commentLikes", new Document("id", commentLikeId));

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 댓글 좋아요 수 스냅샷을 갱신
  public void updateCommentLikeCount(String commentId, long likeCount) {
    Query commentQuery = Query.query(
        Criteria.where("comments.id").is(commentId)
    );

    Update commentUpdate = new Update()
        .set("comments.$.likeCount", likeCount);

    mongoTemplate.updateMulti(commentQuery, commentUpdate, UserActivity.class);

    Query likedCommentQuery = Query.query(
        Criteria.where("commentLikes.commentId").is(commentId)
    );

    Update likedCommentUpdate = new Update()
        .set("commentLikes.$.commentLikeCount", likeCount);

    mongoTemplate.updateMulti(likedCommentQuery, likedCommentUpdate, UserActivity.class);
  }

  // 댓글이 수정되면 좋아요 목록의 댓글 스냅샷도 변경
  public void updateLikeCommentContent(String commentId, String content) {
    Query query = Query.query(
        Criteria.where("commentLikes.commentId").is(commentId)
    );

    Update update = new Update()
        .set("commentLikes.$.commentContent", content);

    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // 댓글이 논리 삭제되면 모든 사용자의 좋아요 목록에서 제거
  public void removeDeletedCommentLikes(String commentId) {
    Query query = Query.query(
        Criteria.where("commentLikes.commentId").is(commentId)
    );

    Update update = new Update()
        .pull("commentLikes", new Document("commentId", commentId));

    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // ==================== 최근 조회 기사 ====================
  // 과거 조회한 기사를 다시 조회할 경우 기존 위치의 기사를 제거
  public void removeExistingArticleView(String userId, String articleId) {
    Query query = findUser(userId);

    Update update = new Update()
        .pull("articleViews", new Document("articleId", articleId));

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  // 조회한 기사를 맨 앞에 추가하고 최근 10건만 유지
  public void addArticleView(
      String userId, ArticleViewActivity articleView
  ) {
    removeExistingArticleView(userId, articleView.getArticleId());

    Query query = findUser(userId);

    Update update = new Update()
        .push("articleViews")
        .atPosition(0)
        .slice(RECENT_ACTIVITY_LIMIT)
        .each(articleView);

    mongoTemplate.updateFirst(query, update, UserActivity.class);

  }

  // 조회 기사에 저장된 댓글 수를 변경
  public void updateArticleViewCommentCount(String articleId, long commentCount) {
    Query query = Query.query(
        Criteria.where("articleViews.articleId").is(articleId)
    );

    Update update = new Update()
        .set("articleViews.$.articleCommentCount", commentCount);

    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // 조회 기사에 저장된 조회 수를 변경합니다
  public void updateArticleViewCount(String articleId, long articleViewCount) {
    Query query = Query.query(
        Criteria.where("articleViews.articleId").is(articleId)
    );

    Update update = new Update()
        .set("articleViews.$.articleViewCount", articleViewCount);

    mongoTemplate.updateMulti(query, update, UserActivity.class);
  }

  // 공통 메소드
  private Query findUser(String userId) {
    return Query.query(Criteria.where("_id").is(userId));
  }
}
