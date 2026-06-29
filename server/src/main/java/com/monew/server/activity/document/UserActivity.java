package com.monew.server.activity.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

//사용자 활동 내역 조회를 위한 MongoDB 역정규화 문서입니다.
//Java의 id 필드는 MongoDB의 _id 필드로 저장되며,
//PostgreSQL users.id와 같은 UUID 문자열을 사용합니다.

@Getter
@NoArgsConstructor
@Document(collection = "user_activities")
public class UserActivity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String nickname;

    private Instant createdAt;

    private List<SubscriptionActivity> subscriptions = new ArrayList<>();

    private List<CommentActivity> comments = new ArrayList<>();

    private List<CommentLikeActivity> commentLikes = new ArrayList<>();

    private List<ArticleViewActivity> articleViews = new ArrayList<>();

    private UserActivity(
        String id,
        String email,
        String nickname,
        Instant createdAt
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.subscriptions = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.commentLikes = new ArrayList<>();
        this.articleViews = new ArrayList<>();
    }

    public static UserActivity create(
        String userId,
        String email,
        String nickname,
        Instant createdAt
    ) {
        return new UserActivity(
            userId,
            email,
            nickname,
            createdAt
        );
    }

//  현재 구독 중인 관심사 정보입니다.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionActivity {

        private String id;
        private String interestId;
        private String interestName;
        private List<String> interestKeywords;
        private Long interestSubscriberCount;
        private Instant createdAt;
    }

//  최근 작성한 댓글 정보입니다. 최대 10건을 유지합니다.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
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

//  최근 좋아요를 누른 댓글 정보입니다. 최대 10건을 유지합니다.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
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

//     * 최근 조회한 기사 정보입니다. 최대 10건을 유지합니다.
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
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