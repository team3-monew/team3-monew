package com.monew.server.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.dto.ArticleViewResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @Mock
    ArticleViewRepository articleViewRepository;

    @Mock
    ArticleQueryRepository articleQueryRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ArticleServiceImpl articleService;

    @Test
    @DisplayName("기사 목록 조회 성공 - limit 초과 조회 결과가 있으면 다음 커서와 hasNext를 생성한다")
    void findArticles_success_hasNext() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null, 2);
        ArticleResponse first = response(UUID.randomUUID(), LocalDateTime.of(2026, 6, 30, 10, 0), 3, 10, false);
        ArticleResponse second = response(UUID.randomUUID(), LocalDateTime.of(2026, 6, 30, 9, 0), 2, 8, true);
        ArticleResponse extra = response(UUID.randomUUID(), LocalDateTime.of(2026, 6, 30, 8, 0), 1, 5, false);
        LocalDateTime secondCreatedAt = LocalDateTime.of(2026, 6, 30, 9, 10);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleQueryRepository.findArticles(condition, userId)).willReturn(List.of(
                new ArticleListQueryResult(first, LocalDateTime.of(2026, 6, 30, 10, 10)),
                new ArticleListQueryResult(second, secondCreatedAt),
                new ArticleListQueryResult(extra, LocalDateTime.of(2026, 6, 30, 8, 10))
        ));
        given(articleQueryRepository.countArticles(condition)).willReturn(3L);

        // when
        CursorPageResponse<ArticleResponse> result = articleService.findArticles(condition, userId);

        // then
        assertThat(result.content()).containsExactly(first, second);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(second.publishDate().toString());
        assertThat(result.nextAfter()).isEqualTo(secondCreatedAt);
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 마지막 페이지이면 다음 커서를 생성하지 않는다")
    void findArticles_success_lastPage() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("viewCount", "DESC", null, null, 3);
        ArticleResponse article = response(UUID.randomUUID(), LocalDateTime.of(2026, 6, 30, 10, 0), 0, 15, false);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleQueryRepository.findArticles(condition, userId)).willReturn(List.of(
                new ArticleListQueryResult(article, LocalDateTime.of(2026, 6, 30, 10, 10))
        ));
        given(articleQueryRepository.countArticles(condition)).willReturn(1L);

        // when
        CursorPageResponse<ArticleResponse> result = articleService.findArticles(condition, userId);

        // then
        assertThat(result.content()).containsExactly(article);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.nextAfter()).isNull();
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 검색 조건이 null이면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_nullCondition() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(null, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);

        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 정렬 기준이 허용되지 않으면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_invalidOrderBy() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("title", "DESC", null, null, 10);

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 정렬 방향이 ASC/DESC가 아니면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_invalidDirection() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("publishDate", "DOWN", null, null, 10);

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - limit이 1 미만이면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_invalidLimitUnderMinimum() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null, 0);

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - limit이 최대값을 넘으면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_invalidLimitOverMaximum() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null, 51);

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 발행일 시작값이 종료값보다 늦으면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_invalidPublishDateRange() {
        // given
        UUID userId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 6, 30, 12, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 30, 11, 0);
        ArticleSearchCondition condition = new ArticleSearchCondition(
                null, null, null, from, to, "publishDate", "DESC", null, null, 10
        );

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - cursor와 after 중 하나만 있으면 커서 예외가 발생한다")
    void findArticles_fail_cursorAfterPairMismatch() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("publishDate", "DESC", "2026-06-30T10:00:00", null, 10);

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_CURSOR);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 조회수 정렬에서 숫자가 아닌 cursor가 들어오면 커서 예외가 발생한다")
    void findArticles_fail_invalidNumberCursor() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "viewCount", "DESC", "not-number", LocalDateTime.of(2026, 6, 30, 10, 0), 10
        );

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_CURSOR);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 발행일 정렬에서 날짜 형식이 아닌 cursor가 들어오면 커서 예외가 발생한다")
    void findArticles_fail_invalidDateTimeCursor() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "publishDate", "DESC", "2026/06/30", LocalDateTime.of(2026, 6, 30, 10, 0), 10
        );

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_CURSOR);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 요청 사용자 ID가 null이면 잘못된 요청 예외가 발생한다")
    void findArticles_fail_nullUserId() {
        // given
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null, 10);

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, null))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 목록 조회 실패 - 요청 사용자가 존재하지 않으면 사용자 없음 예외가 발생한다")
    void findArticles_fail_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null, 10);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticles(condition, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("기사 단건 조회 성공 - 내가 조회한 기사이면 viewedByMe가 true로 응답된다")
    void findArticle_success_viewedByMe() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Article article = article(articleId, ArticleSource.NAVER, "기사 제목", LocalDateTime.of(2026, 6, 30, 10, 0), 2, 7);

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleRepository.findByIdAndDeletedAtIsNull(articleId)).willReturn(Optional.of(article));
        given(articleViewRepository.existsByArticleIdAndUserId(articleId, userId)).willReturn(true);

        // when
        ArticleResponse result = articleService.findArticle(articleId, userId);

        // then
        assertThat(result.id()).isEqualTo(articleId);
        assertThat(result.title()).isEqualTo("기사 제목");
        assertThat(result.viewedByMe()).isTrue();
    }

    @Test
    @DisplayName("기사 단건 조회 실패 - 활성 기사가 없으면 기사 없음 예외가 발생한다")
    void findArticle_fail_articleNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleRepository.findByIdAndDeletedAtIsNull(articleId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> articleService.findArticle(articleId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("기사 조회 등록 성공 - 첫 조회이면 조회 기록을 만들고 조회수를 1 증가시킨다")
    void registerArticleView_success_firstView() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        User user = user(userId);
        Article article = article(articleId, ArticleSource.YEONHAP, "첫 조회 기사", LocalDateTime.of(2026, 6, 30, 10, 0), 0, 0);
        ArticleView articleView = articleView(UUID.randomUUID(), article, user, LocalDateTime.of(2026, 6, 30, 10, 5));

        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
        given(articleViewRepository.insertIgnore(any(UUID.class), any(UUID.class), any(UUID.class))).willReturn(1);
        given(articleRepository.increaseViewCount(articleId)).willReturn(1);
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.of(articleView));

        // when
        ArticleViewResponse result = articleService.registerArticleView(articleId, userId);

        // then
        assertThat(result.articleId()).isEqualTo(articleId);
        assertThat(result.viewedBy()).isEqualTo(userId);
        then(articleRepository).should().increaseViewCount(articleId);
    }

    @Test
    @DisplayName("기사 조회 등록 성공 - 이미 조회한 기사이면 조회수를 다시 증가시키지 않는다")
    void registerArticleView_success_duplicateView() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        User user = user(userId);
        Article article = article(articleId, ArticleSource.CHOSUN, "중복 조회 기사", LocalDateTime.of(2026, 6, 30, 10, 0), 0, 5);
        ArticleView articleView = articleView(UUID.randomUUID(), article, user, LocalDateTime.of(2026, 6, 30, 10, 5));

        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
        given(articleViewRepository.insertIgnore(any(UUID.class), any(UUID.class), any(UUID.class))).willReturn(0);
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.of(articleView));

        // when
        ArticleViewResponse result = articleService.registerArticleView(articleId, userId);

        // then
        assertThat(result.articleId()).isEqualTo(articleId);
        then(articleRepository).should(never()).increaseViewCount(any());
    }

    @Test
    @DisplayName("기사 조회 등록 실패 - 기사 ID가 null이면 기사 없음 예외가 발생한다")
    void registerArticleView_fail_nullArticleId() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        // then
        assertThatThrownBy(() -> articleService.registerArticleView(null, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("기사 조회 등록 실패 - 조회수 증가 결과가 1이 아니면 기사 없음 예외가 발생한다")
    void registerArticleView_fail_viewCountUpdateFailed() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleViewRepository.insertIgnore(any(UUID.class), any(UUID.class), any(UUID.class))).willReturn(1);
        given(articleRepository.increaseViewCount(articleId)).willReturn(0);

        // when
        // then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("기사 조회 등록 실패 - 조회 기록 재조회가 실패하면 잘못된 요청 예외가 발생한다")
    void registerArticleView_fail_articleViewNotFoundAfterInsert() {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();

        given(articleRepository.existsByIdAndDeletedAtIsNull(articleId)).willReturn(true);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user(userId)));
        given(articleViewRepository.insertIgnore(any(UUID.class), any(UUID.class), any(UUID.class))).willReturn(0);
        given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
    }

    @Test
    @DisplayName("기사 논리 삭제 성공 - 활성 기사를 조회한 뒤 deletedAt을 설정한다")
    void softDelete_success() {
        // given
        UUID articleId = UUID.randomUUID();
        Article article = article(articleId, ArticleSource.NAVER, "삭제 대상", LocalDateTime.of(2026, 6, 30, 10, 0), 0, 0);
        given(articleRepository.findActiveByIdForUpdate(articleId)).willReturn(Optional.of(article));

        // when
        articleService.softDelete(articleId);

        // then
        assertThat(article.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("기사 논리 삭제 실패 - 활성 기사가 없으면 기사 없음 예외가 발생한다")
    void softDelete_fail_articleNotFound() {
        // given
        UUID articleId = UUID.randomUUID();
        given(articleRepository.findActiveByIdForUpdate(articleId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> articleService.softDelete(articleId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);
    }

    @Test
    @DisplayName("기사 물리 삭제 성공 - 삭제 여부와 무관하게 기사를 삭제한다")
    void hardDelete_success() {
        // given
        UUID articleId = UUID.randomUUID();
        Article article = article(articleId, ArticleSource.HANKYUNG, "물리 삭제 대상", LocalDateTime.of(2026, 6, 30, 10, 0), 0, 0);
        given(articleRepository.findById(articleId)).willReturn(Optional.of(article));

        // when
        articleService.hardDelete(articleId);

        // then
        then(articleRepository).should().delete(article);
    }

    @Test
    @DisplayName("기사 물리 삭제 실패 - 기사가 없으면 기사 없음 예외가 발생한다")
    void hardDelete_fail_articleNotFound() {
        // given
        UUID articleId = UUID.randomUUID();
        given(articleRepository.findById(articleId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> articleService.hardDelete(articleId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);

        then(articleRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("기사 출처 목록 조회 성공 - 모든 ArticleSource enum 값을 반환한다")
    void findSources_success() {
        // given
        // when
        List<ArticleSource> result = articleService.findSources();

        // then
        assertThat(result).containsExactly(ArticleSource.values());
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
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 6, 30, 9, 0));
        ReflectionTestUtils.setField(user, "updatedAt", LocalDateTime.of(2026, 6, 30, 9, 0));
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
