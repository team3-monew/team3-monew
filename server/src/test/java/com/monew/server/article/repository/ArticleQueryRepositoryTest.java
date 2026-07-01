package com.monew.server.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleInterest;
import com.monew.server.article.entity.ArticleInterestId;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.entity.ArticleView;
import com.monew.server.article.repository.querydsl.ArticleQueryRepository;
import com.monew.server.article.repository.querydsl.ArticleQueryRepositoryImpl;
import com.monew.server.interest.entity.Interest;
import com.monew.server.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaRepositories(basePackageClasses = {ArticleRepository.class, ArticleViewRepository.class})
@EntityScan(basePackageClasses = {Article.class, ArticleView.class, ArticleInterest.class, User.class, Interest.class})
@Import({ArticleQueryRepositoryTest.QuerydslTestConfig.class, ArticleQueryRepositoryImpl.class})
class ArticleQueryRepositoryTest {

    @TestConfiguration
    static class QuerydslTestConfig {

        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Autowired
    ArticleQueryRepository articleQueryRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("기사 목록 조회 성공 - 키워드는 제목과 요약에서 대소문자 구분 없이 검색하고 삭제된 기사는 제외한다")
    void findArticles_success_keywordContainsTitleOrSummaryAndExcludeDeleted() {
        // given
        User user = persistUser("keyword-user@monew.test");
        Article titleMatched = persistArticle("경제 키워드 제목", "일반 요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 0, false);
        Article summaryMatched = persistArticle("일반 제목", "경제 summary", ArticleSource.YEONHAP,
                LocalDateTime.of(2026, 6, 30, 9, 0), LocalDateTime.of(2026, 6, 30, 9, 1), 0, 0, false);
        persistArticle("경제 삭제 기사", "삭제 요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 8, 0), LocalDateTime.of(2026, 6, 30, 8, 1), 0, 0, true);
        persistArticle("정치 기사", "다른 요약", ArticleSource.CHOSUN,
                LocalDateTime.of(2026, 6, 30, 7, 0), LocalDateTime.of(2026, 6, 30, 7, 1), 0, 0, false);
        flushAndClear();

        ArticleSearchCondition condition = condition("경제", null, null, null, null,
                "publishDate", "DESC", null, null, 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(titleMatched.getId(), summaryMatched.getId());
        assertThat(articleQueryRepository.countArticles(condition)).isEqualTo(2);
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 출처와 발행일 범위 조건을 함께 적용한다")
    void findArticles_success_sourceAndPublishDateRange() {
        // given
        User user = persistUser("source-user@monew.test");
        Article expected = persistArticle("범위 포함", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 15, 12, 0), LocalDateTime.of(2026, 6, 15, 12, 1), 0, 0, false);
        persistArticle("출처 제외", "요약", ArticleSource.CHOSUN,
                LocalDateTime.of(2026, 6, 15, 12, 0), LocalDateTime.of(2026, 6, 15, 12, 2), 0, 0, false);
        persistArticle("날짜 제외", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 5, 31, 23, 59), LocalDateTime.of(2026, 5, 31, 23, 59), 0, 0, false);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, List.of(ArticleSource.NAVER),
                LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 30, 23, 59),
                "publishDate", "DESC", null, null, 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(expected.getId());
        assertThat(articleQueryRepository.countArticles(condition)).isEqualTo(1);
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 관심사 ID가 연결된 기사만 조회한다")
    void findArticles_success_interestFilter() {
        // given
        User user = persistUser("interest-user@monew.test");
        Interest economy = persistInterest("경제");
        Interest sports = persistInterest("스포츠");
        Article economyArticle = persistArticle("경제 기사", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 0, false);
        Article sportsArticle = persistArticle("스포츠 기사", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 9, 0), LocalDateTime.of(2026, 6, 30, 9, 1), 0, 0, false);
        persistArticleInterest(economyArticle, economy);
        persistArticleInterest(sportsArticle, sports);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, economy.getId(), null, null, null,
                "publishDate", "DESC", null, null, 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(economyArticle.getId());
        assertThat(articleQueryRepository.countArticles(condition)).isEqualTo(1);
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 사용자가 조회한 기사는 viewedByMe가 true로 매핑된다")
    void findArticles_success_viewedByMe() {
        // given
        User user = persistUser("viewed-user@monew.test");
        Article viewed = persistArticle("조회한 기사", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 0, false);
        Article notViewed = persistArticle("조회하지 않은 기사", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 9, 0), LocalDateTime.of(2026, 6, 30, 9, 1), 0, 0, false);
        persistArticleView(viewed, user);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, null, null, null,
                "publishDate", "DESC", null, null, 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).article().id()).isEqualTo(viewed.getId());
        assertThat(results.get(0).article().viewedByMe()).isTrue();
        assertThat(results.get(1).article().id()).isEqualTo(notViewed.getId());
        assertThat(results.get(1).article().viewedByMe()).isFalse();
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 발행일 내림차순으로 limit보다 1개 더 조회해 hasNext 판단 근거를 제공한다")
    void findArticles_success_publishDateDescLimitPlusOne() {
        // given
        User user = persistUser("limit-user@monew.test");
        Article newest = persistArticle("최신", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 12, 0), LocalDateTime.of(2026, 6, 30, 12, 1), 0, 0, false);
        Article second = persistArticle("두 번째", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 11, 0), LocalDateTime.of(2026, 6, 30, 11, 1), 0, 0, false);
        Article extra = persistArticle("다음 페이지 확인용", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 0, false);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, null, null, null,
                "publishDate", "DESC", null, null, 2);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(newest.getId(), second.getId(), extra.getId());
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 조회수 오름차순 정렬에서 동일 조회수는 createdAt 오름차순으로 정렬한다")
    void findArticles_success_viewCountAscTieBreaker() {
        // given
        User user = persistUser("tie-user@monew.test");
        Article low = persistArticle("조회수 낮음", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 3), 0, 1, false);
        Article sameOld = persistArticle("동일 조회수 먼저", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 5, false);
        Article sameNew = persistArticle("동일 조회수 나중", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 2), 0, 5, false);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, null, null, null,
                "viewCount", "ASC", null, null, 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(low.getId(), sameOld.getId(), sameNew.getId());
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 조회수 내림차순 커서는 동일 조회수에서 createdAt을 타이브레이커로 사용한다")
    void findArticles_success_viewCountDescCursorWithTieBreaker() {
        // given
        User user = persistUser("cursor-user@monew.test");
        persistArticle("첫 페이지 이전", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 4), 0, 30, false);
        Article sameNext = persistArticle("동일 조회수 다음", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 2), 0, 20, false);
        Article lower = persistArticle("낮은 조회수", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 10, false);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, null, null, null,
                "viewCount", "DESC", "20", LocalDateTime.of(2026, 6, 30, 10, 3), 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(sameNext.getId(), lower.getId());
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 댓글수 내림차순으로 정렬한다")
    void findArticles_success_commentCountDesc() {
        // given
        User user = persistUser("comment-user@monew.test");
        Article mostCommented = persistArticle("댓글 많음", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 9, 0), LocalDateTime.of(2026, 6, 30, 9, 1), 9, 0, false);
        Article lessCommented = persistArticle("댓글 적음", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 2, 0, false);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, null, null, null,
                "commentCount", "DESC", null, null, 10);

        // when
        List<ArticleListQueryResult> results = articleQueryRepository.findArticles(condition, user.getId());

        // then
        assertThat(articleIds(results)).containsExactly(mostCommented.getId(), lessCommented.getId());
    }

    @Test
    @DisplayName("기사 개수 조회 성공 - 커서 조건은 전체 개수 계산에 포함하지 않는다")
    void countArticles_success_ignoreCursorCondition() {
        // given
        persistArticle("기사 1", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 10, 0), LocalDateTime.of(2026, 6, 30, 10, 1), 0, 100, false);
        persistArticle("기사 2", "요약", ArticleSource.NAVER,
                LocalDateTime.of(2026, 6, 30, 9, 0), LocalDateTime.of(2026, 6, 30, 9, 1), 0, 50, false);
        flushAndClear();

        ArticleSearchCondition condition = condition(null, null, null, null, null,
                "viewCount", "DESC", "50", LocalDateTime.of(2026, 6, 30, 9, 1), 10);

        // when
        long count = articleQueryRepository.countArticles(condition);

        // then
        assertThat(count).isEqualTo(2);
    }

    private ArticleSearchCondition condition(
            String keyword,
            UUID interestId,
            List<ArticleSource> sourceIn,
            LocalDateTime publishDateFrom,
            LocalDateTime publishDateTo,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit
    ) {
        return new ArticleSearchCondition(
                keyword,
                interestId,
                sourceIn,
                publishDateFrom,
                publishDateTo,
                orderBy,
                direction,
                cursor,
                after,
                limit
        );
    }

    private List<UUID> articleIds(List<ArticleListQueryResult> results) {
        return results.stream()
                .map(result -> result.article().id())
                .toList();
    }

    private User persistUser(String email) {
        User user = new User(email, "테스터", "password");
        setCreatedAt(user, LocalDateTime.of(2026, 6, 30, 0, 0));
        setUpdatedAt(user, LocalDateTime.of(2026, 6, 30, 0, 0));
        entityManager.persist(user);
        return user;
    }

    private Interest persistInterest(String name) {
        Interest interest = new Interest(name + UUID.randomUUID());
        setCreatedAt(interest, LocalDateTime.of(2026, 6, 30, 0, 0));
        entityManager.persist(interest);
        return interest;
    }

    private Article persistArticle(
            String title,
            String summary,
            ArticleSource source,
            LocalDateTime publishDate,
            LocalDateTime createdAt,
            long commentCount,
            long viewCount,
            boolean deleted
    ) {
        Article article = new Article();
        UUID articleId = UUID.randomUUID();

        ReflectionTestUtils.setField(article, "id", articleId);
        ReflectionTestUtils.setField(article, "source", source);
        ReflectionTestUtils.setField(article, "sourceUrl", "https://news.monew.test/" + articleId);
        ReflectionTestUtils.setField(article, "title", title);
        ReflectionTestUtils.setField(article, "publishDate", publishDate);
        ReflectionTestUtils.setField(article, "summary", summary);
        ReflectionTestUtils.setField(article, "commentCount", commentCount);
        ReflectionTestUtils.setField(article, "viewCount", viewCount);

        if (deleted) {
            ReflectionTestUtils.setField(article, "deletedAt", LocalDateTime.of(2026, 6, 30, 23, 59));
        }

        setCreatedAt(article, createdAt);
        setUpdatedAt(article, createdAt);
        entityManager.persist(article);
        return article;
    }

    private void persistArticleView(Article article, User user) {
        ArticleView articleView = new ArticleView(article, user);
        ReflectionTestUtils.setField(articleView, "id", UUID.randomUUID());
        setCreatedAt(articleView, LocalDateTime.of(2026, 6, 30, 11, 0));
        entityManager.persist(articleView);
    }

    private void persistArticleInterest(Article article, Interest interest) {
        ArticleInterestId id = new ArticleInterestId();
        ReflectionTestUtils.setField(id, "articleId", article.getId());
        ReflectionTestUtils.setField(id, "interestId", interest.getId());

        ArticleInterest articleInterest = new ArticleInterest();
        ReflectionTestUtils.setField(articleInterest, "id", id);
        ReflectionTestUtils.setField(articleInterest, "article", article);
        ReflectionTestUtils.setField(articleInterest, "interest", interest);
        setCreatedAt(articleInterest, LocalDateTime.of(2026, 6, 30, 11, 0));

        entityManager.persist(articleInterest);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
    }

    private void setUpdatedAt(Object target, LocalDateTime updatedAt) {
        ReflectionTestUtils.setField(target, "updatedAt", updatedAt);
    }
}