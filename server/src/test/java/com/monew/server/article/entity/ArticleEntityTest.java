package com.monew.server.article.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleEntityTest {

    @Test
    @DisplayName("뉴스 기사 저장 전 처리 성공 - id가 없으면 UUID를 생성한다")
    void articlePrePersist_success_generateId() {
        // Given
        Article article = new Article();

        // When
        ReflectionTestUtils.invokeMethod(article, "prePersist");

        // Then
        assertThat(article.getId()).isNotNull();
    }

    @Test
    @DisplayName("뉴스 기사 저장 전 처리 성공 - 기존 id가 있으면 유지한다")
    void articlePrePersist_success_keepExistingId() {
        // Given
        UUID id = UUID.randomUUID();
        Article article = new Article();
        ReflectionTestUtils.setField(article, "id", id);

        // When
        ReflectionTestUtils.invokeMethod(article, "prePersist");

        // Then
        assertThat(article.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("조회수 증가 성공 - 현재 조회수에서 1 증가한다")
    void increaseViewCount_success() {
        // Given
        Article article = article(2L, 3L);

        // When
        article.increaseViewCount();

        // Then
        assertThat(article.getViewCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("댓글 수 증가 성공 - 현재 댓글 수에서 1 증가한다")
    void increaseCommentCount_success() {
        // Given
        Article article = article(2L, 3L);

        // When
        article.increaseCommentCount();

        // Then
        assertThat(article.getCommentCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("댓글 수 감소 성공 - 댓글 수가 1 이상이면 1 감소한다")
    void decreaseCommentCount_success_positiveCount() {
        // Given
        Article article = article(2L, 3L);

        // When
        article.decreaseCommentCount();

        // Then
        assertThat(article.getCommentCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("댓글 수 감소 성공 - 댓글 수가 0이면 감소하지 않는다")
    void decreaseCommentCount_success_zeroCount() {
        // Given
        Article article = article(0L, 3L);

        // When
        article.decreaseCommentCount();

        // Then
        assertThat(article.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("논리 삭제 성공 - deletedAt이 현재 시각으로 설정된다")
    void softDelete_success() {
        // Given
        Article article = article(0L, 0L);

        // When
        article.softDelete();

        // Then
        assertThat(article.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("기사 조회 엔티티 생성 성공 - 기사와 사용자를 보관한다")
    void articleViewConstructor_success() {
        // Given
        Article article = article(1L, 1L);
        User user = user(UUID.randomUUID());

        // When
        ArticleView articleView = new ArticleView(article, user);

        // Then
        assertThat(articleView.getArticle()).isSameAs(article);
        assertThat(articleView.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("기사 조회 저장 전 처리 성공 - id가 없으면 UUID를 생성한다")
    void articleViewPrePersist_success_generateId() {
        // Given
        ArticleView articleView = new ArticleView(article(1L, 1L), user(UUID.randomUUID()));

        // When
        ReflectionTestUtils.invokeMethod(articleView, "prePersist");

        // Then
        assertThat(articleView.getId()).isNotNull();
    }

    @Test
    @DisplayName("기사 조회 저장 전 처리 성공 - 기존 id가 있으면 유지한다")
    void articleViewPrePersist_success_keepExistingId() {
        // Given
        UUID id = UUID.randomUUID();
        ArticleView articleView = new ArticleView(article(1L, 1L), user(UUID.randomUUID()));
        ReflectionTestUtils.setField(articleView, "id", id);

        // When
        ReflectionTestUtils.invokeMethod(articleView, "prePersist");

        // Then
        assertThat(articleView.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("기사 관심사 복합키 비교 성공 - articleId와 interestId가 같으면 같은 값으로 판단한다")
    void articleInterestIdEquals_success() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        ArticleInterestId first = articleInterestId(articleId, interestId);
        ArticleInterestId second = articleInterestId(articleId, interestId);

        // When
        boolean equals = first.equals(second);

        // Then
        assertThat(equals).isTrue();
        assertThat(first).hasSameHashCodeAs(second);
        assertThat(first.getArticleId()).isEqualTo(articleId);
        assertThat(first.getInterestId()).isEqualTo(interestId);
    }

    @Test
    @DisplayName("기사 관심사 복합키 비교 성공 - 자기 자신과 비교하면 같은 값으로 판단한다")
    void articleInterestIdEquals_success_sameInstance() {
        // Given
        UUID articleId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        ArticleInterestId id = articleInterestId(articleId, interestId);

        // When
        boolean equals = id.equals(id);

        // Then
        assertThat(equals).isTrue();
    }

    @Test
    @DisplayName("기사 관심사 복합키 비교 성공 - 다른 타입과 비교하면 다른 값으로 판단한다")
    void articleInterestIdEquals_success_otherType() {
        // Given
        ArticleInterestId id = articleInterestId(UUID.randomUUID(), UUID.randomUUID());

        // When
        boolean equals = id.equals("invalid");

        // Then
        assertThat(equals).isFalse();
    }

    @Test
    @DisplayName("기사 관심사 복합키 비교 성공 - articleId가 다르면 다른 값으로 판단한다")
    void articleInterestIdEquals_success_differentArticleId() {
        // Given
        UUID interestId = UUID.randomUUID();
        ArticleInterestId first = articleInterestId(UUID.randomUUID(), interestId);
        ArticleInterestId second = articleInterestId(UUID.randomUUID(), interestId);

        // When
        boolean equals = first.equals(second);

        // Then
        assertThat(equals).isFalse();
    }

    @Test
    @DisplayName("기사 관심사 복합키 비교 성공 - interestId가 다르면 다른 값으로 판단한다")
    void articleInterestIdEquals_success_differentInterestId() {
        // Given
        UUID articleId = UUID.randomUUID();
        ArticleInterestId first = articleInterestId(articleId, UUID.randomUUID());
        ArticleInterestId second = articleInterestId(articleId, UUID.randomUUID());

        // When
        boolean equals = first.equals(second);

        // Then
        assertThat(equals).isFalse();
    }

    @Test
    @DisplayName("기사 관심사 엔티티 생성 성공 - 기본 생성 후 필드를 조회할 수 있다")
    void articleInterestNoArgs_success() {
        // Given
        Article article = article(1L, 1L);
        ArticleInterestId id = articleInterestId(article.getId(), UUID.randomUUID());
        ArticleInterest articleInterest = new ArticleInterest();
        ReflectionTestUtils.setField(articleInterest, "id", id);
        ReflectionTestUtils.setField(articleInterest, "article", article);

        // When
        ArticleInterestId resultId = articleInterest.getId();

        // Then
        assertThat(resultId).isEqualTo(id);
        assertThat(articleInterest.getArticle()).isSameAs(article);
    }

    @Test
    @DisplayName("기사 출처 enum 조회 성공 - 지원하는 출처 값을 보관한다")
    void articleSource_success_values() {
        // Given
        ArticleSource[] sources = ArticleSource.values();

        // When
        boolean hasNaver = contains(sources, ArticleSource.NAVER);

        // Then
        assertThat(hasNaver).isTrue();
    }

    private Article article(long commentCount, long viewCount) {
        Article article = new Article();
        UUID articleId = UUID.randomUUID();
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

    private ArticleInterestId articleInterestId(UUID articleId, UUID interestId) {
        ArticleInterestId id = new ArticleInterestId();
        ReflectionTestUtils.setField(id, "articleId", articleId);
        ReflectionTestUtils.setField(id, "interestId", interestId);
        return id;
    }

    private User user(UUID userId) {
        User user = new User("user" + userId + "@example.com", "사용자", "Password1!");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private boolean contains(ArticleSource[] sources, ArticleSource source) {
        for (ArticleSource value : sources) {
            if (value == source) {
                return true;
            }
        }
        return false;
    }
}
