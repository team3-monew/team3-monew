package com.monew.server.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.monew.server.activity.event.ArticleDeletedEvent;
import com.monew.server.activity.event.ArticleViewCountUpdatedEvent;
import com.monew.server.activity.event.ArticleViewedEvent;
import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.entity.ArticleView;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.article.repository.ArticleViewRepository;
import com.monew.server.article.repository.querydsl.ArticleQueryRepository;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceAdditionalTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    ArticleViewRepository articleViewRepository;

    @Mock
    ArticleQueryRepository articleQueryRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    ArticleServiceImpl articleService;

    @Test
    @DisplayName("기사 목록 조회 성공 - 댓글 수 정렬이면 다음 커서를 댓글 수와 기사 ID로 생성한다")
    void findArticles_success_nextCursorCommentCount() {
        // Given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("commentCount", "DESC", null, null, 1);
        ArticleResponse first = response(UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 10, 0), 7L, 3L, false);
        ArticleResponse extra = response(UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 9, 0), 5L, 2L, false);
        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 7, 1, 10, 5);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleQueryRepository.findArticles(condition, userId)).willReturn(List.of(
                new ArticleListQueryResult(first, firstCreatedAt),
                new ArticleListQueryResult(extra, LocalDateTime.of(2026, 7, 1, 9, 5))
        ));
        given(articleQueryRepository.countArticles(condition)).willReturn(2L);

        // When
        CursorPageResponse<ArticleResponse> result = articleService.findArticles(condition, userId);

        // Then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("7|" + first.id());
        assertThat(result.nextAfter()).isEqualTo(firstCreatedAt);
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 조회 수 정렬이면 다음 커서를 조회 수와 기사 ID로 생성한다")
    void findArticles_success_nextCursorViewCount() {
        // Given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("viewCount", "ASC", null, null, 1);
        ArticleResponse first = response(UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 10, 0), 1L, 11L, true);
        ArticleResponse extra = response(UUID.randomUUID(), LocalDateTime.of(2026, 7, 1, 9, 0), 2L, 15L, false);
        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 7, 1, 10, 5);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleQueryRepository.findArticles(condition, userId)).willReturn(List.of(
                new ArticleListQueryResult(first, firstCreatedAt),
                new ArticleListQueryResult(extra, LocalDateTime.of(2026, 7, 1, 9, 5))
        ));
        given(articleQueryRepository.countArticles(condition)).willReturn(2L);

        // When
        CursorPageResponse<ArticleResponse> result = articleService.findArticles(condition, userId);

        // Then
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("11|" + first.id());
        assertThat(result.nextAfter()).isEqualTo(firstCreatedAt);
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 유효한 커서가 있으면 쿼리 저장소로 검색 조건을 전달한다")
    void findArticles_success_validCursor() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        LocalDateTime after = LocalDateTime.of(2026, 7, 1, 10, 5);
        ArticleSearchCondition condition = condition("commentCount", "DESC", "5|" + articleId, after, 10);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleQueryRepository.findArticles(condition, userId)).willReturn(List.of());
        given(articleQueryRepository.countArticles(condition)).willReturn(0L);

        // When
        CursorPageResponse<ArticleResponse> result = articleService.findArticles(condition, userId);

        // Then
        assertThat(result.content()).isEmpty();
        then(articleQueryRepository).should().findArticles(condition, userId);
        then(articleQueryRepository).should().countArticles(condition);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 커서 구분자가 없으면 커서 예외가 발생한다")
    void findArticles_fail_cursorWithoutDelimiter() {
        // Given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "publishDate",
                "DESC",
                "2026-07-01T10:00:00",
                LocalDateTime.of(2026, 7, 1, 10, 5),
                10
        );

        // When
        // Then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_CURSOR);
        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 커서 기사 ID가 UUID 형식이 아니면 커서 예외가 발생한다")
    void findArticles_fail_invalidCursorArticleId() {
        // Given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "publishDate",
                "DESC",
                "2026-07-01T10:00:00|not-uuid",
                LocalDateTime.of(2026, 7, 1, 10, 5),
                10
        );

        // When
        // Then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_CURSOR);
        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("기사 단건 조회 실패 - 사용자 ID가 null이면 잘못된 요청 예외가 발생한다")
    void findArticle_fail_nullUserId() {
        // Given
        UUID articleId = UUID.randomUUID();

        // When
        // Then
        assertThatThrownBy(() -> articleService.findArticle(articleId, null))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
        then(articleRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("기사 단건 조회 실패 - 사용자가 없으면 사용자 없음 예외가 발생한다")
    void findArticle_fail_userNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> articleService.findArticle(articleId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        then(articleRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("기사 단건 조회 성공 - 내가 조회하지 않은 기사이면 viewedByMe가 false로 응답된다")
    void findArticle_success_notViewedByMe() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(articleId, ArticleSource.NAVER, "미조회 기사", LocalDateTime.of(2026, 7, 1, 10, 0), 1L, 2L);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleRepository.findByIdAndDeletedAtIsNull(articleId)).willReturn(Optional.of(article));
        given(articleViewRepository.existsByArticleIdAndUserId(articleId, userId)).willReturn(false);

        // When
        ArticleResponse result = articleService.findArticle(articleId, userId);

        // Then
        assertThat(result.id()).isEqualTo(articleId);
        assertThat(result.viewedByMe()).isFalse();
    }

    @Test
    @DisplayName("기사 조회 등록 실패 - 활성 기사가 없으면 기사 없음 예외가 발생한다")
    void registerArticleView_fail_articleNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(false);

        // When
        // Then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);
        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("기사 조회 등록 실패 - 사용자가 없으면 사용자 없음 예외가 발생한다")
    void registerArticleView_fail_userNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // When
        // Then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        then(articleViewRepository).should(never()).insertIgnore(any(), any(), any());
    }

    @Test
    @DisplayName("기사 조회 등록 성공 - 첫 조회이면 조회 이벤트와 조회수 변경 이벤트를 발행한다")
    void registerArticleView_success_publishEventsFirstView() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        User user = user(userId);
        Article article = article(articleId, ArticleSource.YEONHAP, "이벤트 기사", LocalDateTime.of(2026, 7, 1, 10, 0), 0L, 9L);
        ArticleView articleView = articleView(UUID.randomUUID(), article, user, LocalDateTime.of(2026, 7, 1, 10, 5));

        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
        given(articleViewRepository.insertIgnore(any(UUID.class), eq(articleId), eq(userId))).willReturn(1);
        given(articleRepository.increaseViewCount(articleId)).willReturn(1);
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.of(articleView));

        // When
        articleService.registerArticleView(articleId, userId);

        // Then
        then(eventPublisher).should().publishEvent(any(ArticleViewedEvent.class));
        then(eventPublisher).should().publishEvent(any(ArticleViewCountUpdatedEvent.class));
    }

    @Test
    @DisplayName("기사 조회 등록 성공 - 중복 조회이면 조회 이벤트만 발행하고 조회수 변경 이벤트는 발행하지 않는다")
    void registerArticleView_success_publishOnlyViewedEventDuplicateView() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        User user = user(userId);
        Article article = article(articleId, ArticleSource.CHOSUN, "중복 이벤트 기사", LocalDateTime.of(2026, 7, 1, 10, 0), 0L, 9L);
        ArticleView articleView = articleView(UUID.randomUUID(), article, user, LocalDateTime.of(2026, 7, 1, 10, 5));

        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
        given(articleViewRepository.insertIgnore(any(UUID.class), eq(articleId), eq(userId))).willReturn(0);
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.of(articleView));

        // When
        articleService.registerArticleView(articleId, userId);

        // Then
        then(eventPublisher).should().publishEvent(any(ArticleViewedEvent.class));
        then(eventPublisher).should(never()).publishEvent(any(ArticleViewCountUpdatedEvent.class));
    }

    @Test
    @DisplayName("기사 논리 삭제 성공 - 삭제 이벤트를 발행한다")
    void softDelete_success_publishEvent() {
        // Given
        UUID articleId = UUID.randomUUID();
        Article article = article(articleId, ArticleSource.HANKYUNG, "삭제 이벤트 기사", LocalDateTime.of(2026, 7, 1, 10, 0), 0L, 0L);
        given(articleRepository.findActiveByIdForUpdate(articleId)).willReturn(Optional.of(article));

        // When
        articleService.softDelete(articleId);

        // Then
        assertThat(article.getDeletedAt()).isNotNull();
        then(eventPublisher).should().publishEvent(any(ArticleDeletedEvent.class));
    }

    private ArticleSearchCondition condition(
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit
    ) {
        return new ArticleSearchCondition(
                null,
                null,
                null,
                null,
                null,
                orderBy,
                direction,
                cursor,
                after,
                limit
        );
    }

    private ArticleResponse response(
            UUID articleId,
            LocalDateTime publishDate,
            long commentCount,
            long viewCount,
            boolean viewedByMe
    ) {
        return new ArticleResponse(
                articleId,
                ArticleSource.NAVER,
                "https://news.monew.test/articles/" + articleId,
                "기사 제목 " + articleId,
                publishDate,
                "기사 요약",
                commentCount,
                viewCount,
                viewedByMe
        );
    }

    private Article article(
            UUID articleId,
            ArticleSource source,
            String title,
            LocalDateTime publishDate,
            long commentCount,
            long viewCount
    ) {
        Article article = new Article();
        ReflectionTestUtils.setField(article, "id", articleId);
        ReflectionTestUtils.setField(article, "source", source);
        ReflectionTestUtils.setField(article, "sourceUrl", "https://news.monew.test/articles/" + articleId);
        ReflectionTestUtils.setField(article, "title", title);
        ReflectionTestUtils.setField(article, "publishDate", publishDate);
        ReflectionTestUtils.setField(article, "summary", "기사 요약");
        ReflectionTestUtils.setField(article, "commentCount", commentCount);
        ReflectionTestUtils.setField(article, "viewCount", viewCount);
        ReflectionTestUtils.setField(article, "createdAt", publishDate.plusMinutes(1));
        ReflectionTestUtils.setField(article, "updatedAt", publishDate.plusMinutes(1));
        return article;
    }

    private User user(UUID userId) {
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 7, 1, 9, 0));
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.of(2026, 7, 1, 9, 0));
        return user;
    }

    private ArticleView articleView(
            UUID articleViewId,
            Article article,
            User user,
            LocalDateTime createdAt
    ) {
        ArticleView articleView = new ArticleView(article, user);
        ReflectionTestUtils.setField(articleView, "id", articleViewId);
        ReflectionTestUtils.setField(articleView, "createdAt", createdAt);
        return articleView;
    }
}
