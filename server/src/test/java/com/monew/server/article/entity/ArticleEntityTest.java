package com.monew.server.article.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArticleEntityTest {

    @Test
    @DisplayName("조회수 증가 성공 - 현재 조회수에서 1 증가한다")
    void increaseViewCount_success() {
        // given
        Article article = article(3L);

        // when
        article.increaseViewCount();

        // then
        assertThat(article.getViewCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("논리 삭제 성공 - deletedAt이 현재 시각으로 설정된다")
    void softDelete_success() {
        // given
        Article article = article(0L);

        // when
        article.softDelete();

        // then
        assertThat(article.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("ArticleResponse 변환 성공 - 엔티티 필드와 조회 여부를 응답 DTO에 매핑한다")
    void articleResponseFrom_success() {
        // given
        Article article = article(11L);

        // when
        com.monew.server.article.dto.ArticleResponse result =
                com.monew.server.article.dto.ArticleResponse.from(article, true);

        // then
        assertThat(result.id()).isEqualTo(article.getId());
        assertThat(result.source()).isEqualTo(article.getSource());
        assertThat(result.title()).isEqualTo(article.getTitle());
        assertThat(result.viewCount()).isEqualTo(11L);
        assertThat(result.viewedByMe()).isTrue();
    }

    private Article article(long viewCount) {
        Article article = new Article();
        UUID articleId = UUID.randomUUID();
        ReflectionTestUtils.setField(article, "id", articleId);
        ReflectionTestUtils.setField(article, "source", ArticleSource.NAVER);
        ReflectionTestUtils.setField(article, "sourceUrl", "https://news.monew.test/articles/" + articleId);
        ReflectionTestUtils.setField(article, "title", "기사 제목");
        ReflectionTestUtils.setField(article, "publishDate", LocalDateTime.of(2026, 6, 30, 10, 0));
        ReflectionTestUtils.setField(article, "summary", "기사 요약");
        ReflectionTestUtils.setField(article, "commentCount", 2L);
        ReflectionTestUtils.setField(article, "viewCount", viewCount);
        return article;
    }
}
