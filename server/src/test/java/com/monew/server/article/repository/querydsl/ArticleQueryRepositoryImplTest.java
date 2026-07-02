package com.monew.server.article.repository.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.entity.ArticleSource;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleQueryRepositoryImplTest {

    @Test
    @DisplayName("키워드 조건 생성 성공 - keyword가 null이면 조건을 생성하지 않는다")
    void keywordContains_success_nullKeyword() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "keywordContains", (String) null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("키워드 조건 생성 성공 - keyword가 공백이면 조건을 생성하지 않는다")
    void keywordContains_success_blankKeyword() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "keywordContains", "   ");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("키워드 조건 생성 성공 - keyword가 있으면 제목 또는 요약 검색 조건을 생성한다")
    void keywordContains_success_validKeyword() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "keywordContains", "AI");

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("관심사 조건 생성 성공 - interestId가 null이면 조건을 생성하지 않는다")
    void interestEq_success_nullInterestId() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "interestEq", (UUID) null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("관심사 조건 생성 성공 - interestId가 있으면 EXISTS 조건을 생성한다")
    void interestEq_success_validInterestId() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "interestEq", UUID.randomUUID());

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("출처 조건 생성 성공 - sourceIn이 null이면 조건을 생성하지 않는다")
    void sourceIn_success_nullSources() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "sourceIn", (List<ArticleSource>) null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("출처 조건 생성 성공 - sourceIn이 비어 있으면 조건을 생성하지 않는다")
    void sourceIn_success_emptySources() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "sourceIn", List.of());

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("출처 조건 생성 성공 - sourceIn이 있으면 IN 조건을 생성한다")
    void sourceIn_success_validSources() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "sourceIn", List.of(ArticleSource.NAVER));

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("발행일 시작 조건 생성 성공 - publishDateFrom이 null이면 조건을 생성하지 않는다")
    void publishDateGoe_success_nullPublishDateFrom() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "publishDateGoe", (LocalDateTime) null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("발행일 시작 조건 생성 성공 - publishDateFrom이 있으면 goe 조건을 생성한다")
    void publishDateGoe_success_validPublishDateFrom() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        LocalDateTime publishDateFrom = LocalDateTime.of(2026, 7, 1, 0, 0);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "publishDateGoe", publishDateFrom);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("발행일 종료 조건 생성 성공 - publishDateTo가 null이면 조건을 생성하지 않는다")
    void publishDateLoe_success_nullPublishDateTo() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "publishDateLoe", (LocalDateTime) null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("발행일 종료 조건 생성 성공 - publishDateTo가 있으면 loe 조건을 생성한다")
    void publishDateLoe_success_validPublishDateTo() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        LocalDateTime publishDateTo = LocalDateTime.of(2026, 7, 1, 23, 59);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "publishDateLoe", publishDateTo);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - cursor가 null이면 조건을 생성하지 않는다")
    void cursorCondition_success_nullCursor() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", condition);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - cursor가 공백이면 조건을 생성하지 않는다")
    void cursorCondition_success_blankCursor() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        ArticleSearchCondition condition = condition("publishDate", "DESC", "   ", null);

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", condition);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - 발행일 내림차순 cursor와 after가 있으면 조건을 생성한다")
    void cursorCondition_success_publishDateDescWithAfter() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        UUID articleId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "publishDate",
                "DESC",
                "2026-07-01T10:00:00|" + articleId,
                LocalDateTime.of(2026, 7, 1, 10, 5)
        );

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", condition);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - 발행일 오름차순 cursor만 있으면 조건을 생성한다")
    void cursorCondition_success_publishDateAscWithoutAfter() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        UUID articleId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "publishDate",
                "ASC",
                "2026-07-01T10:00:00|" + articleId,
                null
        );

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", condition);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - 댓글 수 오름차순 cursor와 after가 있으면 조건을 생성한다")
    void cursorCondition_success_commentCountAscWithAfter() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        UUID articleId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "commentCount",
                "ASC",
                "5|" + articleId,
                LocalDateTime.of(2026, 7, 1, 10, 5)
        );

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", condition);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("커서 조건 생성 성공 - 조회 수 내림차순 cursor와 after가 있으면 조건을 생성한다")
    void cursorCondition_success_viewCountDescWithAfter() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        UUID articleId = UUID.randomUUID();
        ArticleSearchCondition condition = condition(
                "viewCount",
                "DESC",
                "10|" + articleId,
                LocalDateTime.of(2026, 7, 1, 10, 5)
        );

        // When
        BooleanExpression result = ReflectionTestUtils.invokeMethod(repository, "cursorCondition", condition);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("커서 파싱 실패 - 구분자가 없으면 예외가 발생한다")
    void parseArticleCursor_fail_noDelimiter() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        // Then
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(repository, "parseArticleCursor", "invalid-cursor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid cursor format");
    }

    @Test
    @DisplayName("커서 파싱 실패 - 정렬 값이 비어 있으면 예외가 발생한다")
    void parseArticleCursor_fail_blankSortValue() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        // Then
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(repository, "parseArticleCursor", " |" + UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid cursor format");
    }

    @Test
    @DisplayName("커서 파싱 실패 - 기사 ID가 비어 있으면 예외가 발생한다")
    void parseArticleCursor_fail_blankArticleId() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        // Then
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(repository, "parseArticleCursor", "2026-07-01T10:00:00| "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid cursor format");
    }

    @Test
    @DisplayName("정렬 조건 생성 성공 - 발행일 정렬이면 발행일, 생성일, ID 순으로 정렬 조건을 생성한다")
    void orderSpecifiers_success_publishDate() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        ArticleSearchCondition condition = condition("publishDate", "DESC", null, null);

        // When
        OrderSpecifier<?>[] result = ReflectionTestUtils.invokeMethod(repository, "orderSpecifiers", condition);

        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("정렬 조건 생성 성공 - 댓글 수 정렬이면 댓글 수, 생성일, ID 순으로 정렬 조건을 생성한다")
    void orderSpecifiers_success_commentCount() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        ArticleSearchCondition condition = condition("commentCount", "ASC", null, null);

        // When
        OrderSpecifier<?>[] result = ReflectionTestUtils.invokeMethod(repository, "orderSpecifiers", condition);

        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("정렬 조건 생성 성공 - 조회 수 정렬이면 조회 수, 생성일, ID 순으로 정렬 조건을 생성한다")
    void orderSpecifiers_success_viewCount() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);
        ArticleSearchCondition condition = condition("viewCount", "DESC", null, null);

        // When
        OrderSpecifier<?>[] result = ReflectionTestUtils.invokeMethod(repository, "orderSpecifiers", condition);

        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("정렬 방향 판별 성공 - ASC는 오름차순으로 판단한다")
    void isAsc_success_asc() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        Boolean result = ReflectionTestUtils.invokeMethod(repository, "isAsc", "ASC");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정렬 방향 판별 성공 - DESC는 내림차순으로 판단한다")
    void isAsc_success_desc() {
        // Given
        ArticleQueryRepositoryImpl repository = new ArticleQueryRepositoryImpl(null);

        // When
        Boolean result = ReflectionTestUtils.invokeMethod(repository, "isAsc", "DESC");

        // Then
        assertThat(result).isFalse();
    }

    private ArticleSearchCondition condition(
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after
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
                10
        );
    }
}