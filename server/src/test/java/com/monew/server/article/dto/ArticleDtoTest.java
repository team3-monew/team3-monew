package com.monew.server.article.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.entity.ArticleView;
import com.monew.server.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleDtoTest {

    @Test
    @DisplayName("기사 목록 쿼리 결과 DTO 생성 성공 - 기사 응답과 생성일을 보관한다")
    void articleListQueryResult_success() {
        // Given
        ArticleResponse article = articleResponse(UUID.randomUUID(), false);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);

        // When
        ArticleListQueryResult result = new ArticleListQueryResult(article, createdAt);

        // Then
        assertThat(result.article()).isSameAs(article);
        assertThat(result.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("기사 응답 DTO 생성 성공 - 입력값을 그대로 보관한다")
    void articleResponse_success() {
        // Given
        UUID articleId = UUID.randomUUID();
        LocalDateTime publishDate = LocalDateTime.of(2026, 7, 1, 10, 0);

        // When
        ArticleResponse response = new ArticleResponse(
                articleId,
                ArticleSource.CHOSUN,
                "https://news.monew.test/articles/" + articleId,
                "기사 제목",
                publishDate,
                "기사 요약",
                2L,
                3L,
                true
        );

        // Then
        assertThat(response.id()).isEqualTo(articleId);
        assertThat(response.source()).isEqualTo(ArticleSource.CHOSUN);
        assertThat(response.sourceUrl()).isEqualTo("https://news.monew.test/articles/" + articleId);
        assertThat(response.title()).isEqualTo("기사 제목");
        assertThat(response.publishDate()).isEqualTo(publishDate);
        assertThat(response.summary()).isEqualTo("기사 요약");
        assertThat(response.commentCount()).isEqualTo(2L);
        assertThat(response.viewCount()).isEqualTo(3L);
        assertThat(response.viewedByMe()).isTrue();
    }

    @Test
    @DisplayName("기사 응답 변환 성공 - 엔티티 필드와 조회 여부를 응답 DTO에 매핑한다")
    void articleResponseFrom_success() {
        // Given
        Article article = article(UUID.randomUUID(), 5L, 9L);

        // When
        ArticleResponse response = ArticleResponse.from(article, true);

        // Then
        assertThat(response.id()).isEqualTo(article.getId());
        assertThat(response.source()).isEqualTo(article.getSource());
        assertThat(response.sourceUrl()).isEqualTo(article.getSourceUrl());
        assertThat(response.title()).isEqualTo(article.getTitle());
        assertThat(response.publishDate()).isEqualTo(article.getPublishDate());
        assertThat(response.summary()).isEqualTo(article.getSummary());
        assertThat(response.commentCount()).isEqualTo(5L);
        assertThat(response.viewCount()).isEqualTo(9L);
        assertThat(response.viewedByMe()).isTrue();
    }

    @Test
    @DisplayName("기사 검색 조건 DTO 생성 성공 - 검색 파라미터를 그대로 보관한다")
    void articleSearchCondition_success() {
        // Given
        UUID interestId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 31, 23, 59);
        LocalDateTime after = LocalDateTime.of(2026, 7, 15, 10, 0);

        // When
        ArticleSearchCondition condition = new ArticleSearchCondition(
                "AI",
                interestId,
                List.of(ArticleSource.NAVER, ArticleSource.HANKYUNG),
                from,
                to,
                "publishDate",
                "DESC",
                "2026-07-15T10:00:00|" + UUID.randomUUID(),
                after,
                20
        );

        // Then
        assertThat(condition.keyword()).isEqualTo("AI");
        assertThat(condition.interestId()).isEqualTo(interestId);
        assertThat(condition.sourceIn()).containsExactly(ArticleSource.NAVER, ArticleSource.HANKYUNG);
        assertThat(condition.publishDateFrom()).isEqualTo(from);
        assertThat(condition.publishDateTo()).isEqualTo(to);
        assertThat(condition.orderBy()).isEqualTo("publishDate");
        assertThat(condition.direction()).isEqualTo("DESC");
        assertThat(condition.cursor()).isNotBlank();
        assertThat(condition.after()).isEqualTo(after);
        assertThat(condition.limit()).isEqualTo(20);
    }

    @Test
    @DisplayName("기사 조회 응답 변환 성공 - 조회 엔티티와 기사 정보를 응답 DTO에 매핑한다")
    void articleViewResponseFrom_success() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID articleViewId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 5);
        Article article = article(articleId, 4L, 6L);
        User user = user(userId);
        ArticleView articleView = new ArticleView(article, user);
        ReflectionTestUtils.setField(articleView, "id", articleViewId);
        ReflectionTestUtils.setField(articleView, "createdAt", createdAt);

        // When
        ArticleViewResponse response = ArticleViewResponse.from(articleView);

        // Then
        assertThat(response.id()).isEqualTo(articleViewId);
        assertThat(response.viewedBy()).isEqualTo(userId);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.articleId()).isEqualTo(articleId);
        assertThat(response.source()).isEqualTo(article.getSource());
        assertThat(response.sourceUrl()).isEqualTo(article.getSourceUrl());
        assertThat(response.articleTitle()).isEqualTo(article.getTitle());
        assertThat(response.articlePublishedDate()).isEqualTo(article.getPublishDate());
        assertThat(response.articleSummary()).isEqualTo(article.getSummary());
        assertThat(response.articleCommentCount()).isEqualTo(4L);
        assertThat(response.articleViewCount()).isEqualTo(6L);
    }

    private ArticleResponse articleResponse(UUID articleId, boolean viewedByMe) {
        return new ArticleResponse(
                articleId,
                ArticleSource.NAVER,
                "https://news.monew.test/articles/" + articleId,
                "기사 제목",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                "기사 요약",
                1L,
                2L,
                viewedByMe
        );
    }

    private Article article(UUID articleId, long commentCount, long viewCount) {
        Article article = new Article();
        ReflectionTestUtils.setField(article, "id", articleId);
        ReflectionTestUtils.setField(article, "source", ArticleSource.NAVER);
        ReflectionTestUtils.setField(article, "sourceUrl", "https://news.monew.test/articles/" + articleId);
        ReflectionTestUtils.setField(article, "title", "기사 제목");
        ReflectionTestUtils.setField(article, "publishDate", LocalDateTime.of(2026, 7, 1, 10, 0));
        ReflectionTestUtils.setField(article, "summary", "기사 요약");
        ReflectionTestUtils.setField(article, "commentCount", commentCount);
        ReflectionTestUtils.setField(article, "viewCount", viewCount);
        return article;
    }

    private User user(UUID userId) {
        User user = new User("user" + userId + "@example.com", "사용자", "Password1!");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
