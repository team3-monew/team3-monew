package com.monew.server.activity.dto;

import com.monew.server.activity.document.UserActivity;
import java.time.LocalDateTime;
import java.util.List;

public record UserActivityResponse(
    String id,
    String email,
    String nickname,
    LocalDateTime createdAt,
    List<UserActivity.SubscriptionActivity> subscriptions,
    List<UserActivity.CommentActivity> comments,
    List<UserActivity.CommentLikeActivity> commentLikes,
    List<UserActivity.ArticleViewActivity> articleViews
) {

  public static UserActivityResponse from(UserActivity activity) {
    return new UserActivityResponse(
        activity.getId(),
        activity.getEmail(),
        activity.getNickname(),
        activity.getCreatedAt(),
        activity.getSubscriptions(),
        activity.getComments(),
        activity.getCommentLikes(),
        activity.getArticleViews()
    );
  }
}
