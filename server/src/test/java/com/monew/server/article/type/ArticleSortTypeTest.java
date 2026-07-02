package com.monew.server.article.type;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.article.type.ArticleSortType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleSortTypeTest {

    @Test
    @DisplayName("정렬 기준 검증 성공 - 허용된 문자열이면 true를 반환한다")
    void isValid_success() {
        // given
        String publishDate = "publishDate";
        String commentCount = "commentCount";
        String viewCount = "viewCount";

        // when
        boolean publishDateValid = ArticleSortType.isValid(publishDate);
        boolean commentCountValid = ArticleSortType.isValid(commentCount);
        boolean viewCountValid = ArticleSortType.isValid(viewCount);

        // then
        assertThat(publishDateValid).isTrue();
        assertThat(commentCountValid).isTrue();
        assertThat(viewCountValid).isTrue();
    }

    @Test
    @DisplayName("정렬 기준 검증 실패 - 허용되지 않은 문자열이면 false를 반환한다")
    void isValid_fail() {
        // given
        String invalidOrderBy = "title";
        String nullOrderBy = null;

        // when
        boolean invalidOrderByValid = ArticleSortType.isValid(invalidOrderBy);
        boolean nullOrderByValid = ArticleSortType.isValid(nullOrderBy);

        // then
        assertThat(invalidOrderByValid).isFalse();
        assertThat(nullOrderByValid).isFalse();
    }

    @Test
    @DisplayName("정렬 기준 변환 성공 - 문자열에 맞는 enum을 반환한다")
    void from_success() {
        // given
        String publishDate = "publishDate";
        String commentCount = "commentCount";
        String viewCount = "viewCount";

        // when
        ArticleSortType publishDateSortType = ArticleSortType.from(publishDate);
        ArticleSortType commentCountSortType = ArticleSortType.from(commentCount);
        ArticleSortType viewCountSortType = ArticleSortType.from(viewCount);

        // then
        assertThat(publishDateSortType).isEqualTo(ArticleSortType.PUBLISH_DATE);
        assertThat(commentCountSortType).isEqualTo(ArticleSortType.COMMENT_COUNT);
        assertThat(viewCountSortType).isEqualTo(ArticleSortType.VIEW_COUNT);
    }

    @Test
    @DisplayName("정렬 기준 변환 실패 - 허용되지 않은 문자열이면 IllegalArgumentException이 발생한다")
    void from_fail() {
        // given
        String invalidOrderBy = "title";

        // when
        Throwable throwable = org.assertj.core.api.Assertions.catchThrowable(
                () -> ArticleSortType.from(invalidOrderBy)
        );

        // then
        assertThat(throwable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid article sort type");
    }
}
