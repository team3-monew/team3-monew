package com.monew.server.activity.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** PostgreSQL 사용자 활동 데이터를 조회용으로 비정규화한 MongoDB 문서입니다. */
@Getter
@NoArgsConstructor
@Document(collection = "user_activities")
public class UserActivity {

    @Id
    private String mongoId;

    // 제공된 validator는 `id` 필드를 요구하고, 조회 키는 MongoDB의 `_id`입니다.
    // 동기화 시 두 값에 같은 PostgreSQL users.id(UUID)를 넣습니다.
    @Field("id")
    private String id;

    @Indexed(unique = true)
    private String email;

    private String nickname;
    private Instant createdAt;
    private List<SubscriptionActivity> subscriptions = new ArrayList<>();
    private List<CommentActivity> comments = new ArrayList<>();
    private List<CommentLikeActivity> commentLikes = new ArrayList<>();
    private List<ArticleViewActivity> articleViews = new ArrayList<>();

    @Getter @NoArgsConstructor
    public static class SubscriptionActivity {
        private String id;
        private String interestId;
        private String interestName;
        private List<String> interestKeywords = new ArrayList<>();
        private Long interestSubscriberCount;
        private Instant createdAt;
    }

    @Getter @NoArgsConstructor
    public static class CommentActivity {
        private String id;
        private String articleId;
        private String articleTitle;
        private String userId;
        private String userNickname;
        private String content;
        private Long likeCount;
        private Instant createdAt;
    }

    @Getter @NoArgsConstructor
    public static class CommentLikeActivity {
        private String id;
        private Instant createdAt;
        private String commentId;
        private String articleId;
        private String articleTitle;
        private String commentUserId;
        private String commentUserNickname;
        private String commentContent;
        private Long commentLikeCount;
        private Instant commentCreatedAt;
    }

    @Getter @NoArgsConstructor
    public static class ArticleViewActivity {
        private String id;
        private String viewedBy;
        private Instant createdAt;
        private String articleId;
        private String source;
        private String sourceUrl;
        private String articleTitle;
        private Instant articlePublishedDate;
        private String articleSummary;
        private Long articleCommentCount;
        private Long articleViewCount;
    }
}
