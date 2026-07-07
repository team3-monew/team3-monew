package com.monew.server.article.repository.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.article.dto.ArticleBackupDto;
import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleInterest;
import com.monew.server.article.entity.ArticleInterestId;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.entity.ArticleView;
import com.monew.server.interest.entity.Interest;
import com.monew.server.support.RepositoryTestSupport;
import com.monew.server.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@Import(ArticleQueryRepositoryImpl.class)
class ArticleQueryRepositoryIntegrationTest extends RepositoryTestSupport {

    @Autowired
    private ArticleQueryRepository articleQueryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기사 목록 조회 성공 - 삭제 기사 제외, 출처/키워드/발행일 조건을 적용하고 publishDate 내림차순으로 조회한다")
    void findArticles_success_filterAndSortByPublishDateDesc() {
        // Given
        User user = saveUser("article-user1@test.com", "articleUser1");

        LocalDateTime base = LocalDateTime.of(2026, 7, 6, 10, 0);

        Article oldArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000101"),
                ArticleSource.NAVER,
                "https://news.test/old",
                "AI 오래된 기사",
                "AI 요약",
                base.minusDays(2),
                base.minusDays(2),
                1,
                10,
                null
        );

        Article newestArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000102"),
                ArticleSource.NAVER,
                "https://news.test/newest",
                "AI 최신 기사",
                "AI 최신 요약",
                base,
                base,
                2,
                30,
                null
        );

        Article otherSource = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000103"),
                ArticleSource.CHOSUN,
                "https://news.test/chosun",
                "AI 조선 기사",
                "AI 조선 요약",
                base.plusDays(1),
                base.plusDays(1),
                3,
                40,
                null
        );

        Article deletedArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000104"),
                ArticleSource.NAVER,
                "https://news.test/deleted",
                "AI 삭제 기사",
                "AI 삭제 요약",
                base.plusDays(2),
                base.plusDays(2),
                4,
                50,
                base.plusDays(3)
        );

        saveArticleView(newestArticle, user);

        entityManager.flush();
        entityManager.clear();

        ArticleSearchCondition condition = new ArticleSearchCondition(
                "AI",
                null,
                List.of(ArticleSource.NAVER),
                base.minusDays(3),
                base.plusDays(3),
                "publishDate",
                "DESC",
                null,
                null,
                10
        );

        // When
        List<ArticleListQueryResult> result = articleQueryRepository.findArticles(condition, user.getId());

        // Then
        assertThat(result)
                .extracting(row -> row.article().id())
                .containsExactly(newestArticle.getId(), oldArticle.getId());

        assertThat(result)
                .extracting(row -> row.article().id())
                .doesNotContain(otherSource.getId(), deletedArticle.getId());

        assertThat(result.get(0).article().viewedByMe()).isTrue();
        assertThat(result.get(1).article().viewedByMe()).isFalse();
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - 관심사 조건을 적용한다")
    void findArticles_success_interestFilter() {
        // Given
        User user = saveUser("article-user2@test.com", "articleUser2");
        Interest interest = saveInterest("인공지능");

        LocalDateTime base = LocalDateTime.of(2026, 7, 6, 10, 0);

        Article matchedArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000111"),
                ArticleSource.NAVER,
                "https://news.test/interest-matched",
                "관심사 매칭 기사",
                "관심사 요약",
                base,
                base,
                1,
                10,
                null
        );

        Article notMatchedArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000112"),
                ArticleSource.NAVER,
                "https://news.test/interest-not-matched",
                "관심사 미매칭 기사",
                "관심사 요약",
                base.plusDays(1),
                base.plusDays(1),
                1,
                10,
                null
        );

        saveArticleInterest(matchedArticle, interest);

        entityManager.flush();
        entityManager.clear();

        ArticleSearchCondition condition = new ArticleSearchCondition(
                null,
                interest.getId(),
                null,
                null,
                null,
                "publishDate",
                "DESC",
                null,
                null,
                10
        );

        // When
        List<ArticleListQueryResult> result = articleQueryRepository.findArticles(condition, user.getId());

        // Then
        assertThat(result)
                .extracting(row -> row.article().id())
                .containsExactly(matchedArticle.getId());

        assertThat(result)
                .extracting(row -> row.article().id())
                .doesNotContain(notMatchedArticle.getId());
    }

    @Test
    @DisplayName("기사 목록 조회 성공 - publishDate 커서 이후 기사만 조회한다")
    void findArticles_success_publishDateCursor() {
        // Given
        User user = saveUser("article-user3@test.com", "articleUser3");

        LocalDateTime cursorPublishDate = LocalDateTime.of(2026, 7, 6, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 6, 11, 0);

        Article cursorArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000121"),
                ArticleSource.NAVER,
                "https://news.test/cursor",
                "커서 기사",
                "요약",
                cursorPublishDate,
                createdAt,
                1,
                10,
                null
        );

        Article newerArticle = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000122"),
                ArticleSource.NAVER,
                "https://news.test/newer",
                "커서 이전 기사",
                "요약",
                cursorPublishDate.plusDays(1),
                createdAt.plusDays(1),
                1,
                10,
                null
        );

        Article olderArticle1 = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000123"),
                ArticleSource.NAVER,
                "https://news.test/older-1",
                "커서 이후 기사 1",
                "요약",
                cursorPublishDate.minusDays(1),
                createdAt.minusDays(1),
                1,
                10,
                null
        );

        Article olderArticle2 = saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000124"),
                ArticleSource.NAVER,
                "https://news.test/older-2",
                "커서 이후 기사 2",
                "요약",
                cursorPublishDate.minusDays(2),
                createdAt.minusDays(2),
                1,
                10,
                null
        );

        entityManager.flush();
        entityManager.clear();

        ArticleSearchCondition condition = new ArticleSearchCondition(
                null,
                null,
                null,
                null,
                null,
                "publishDate",
                "DESC",
                cursorPublishDate + "|" + cursorArticle.getId(),
                null,
                10
        );

        // When
        List<ArticleListQueryResult> result = articleQueryRepository.findArticles(condition, user.getId());

        // Then
        assertThat(result)
                .extracting(row -> row.article().id())
                .containsExactly(olderArticle1.getId(), olderArticle2.getId());

        assertThat(result)
                .extracting(row -> row.article().id())
                .doesNotContain(cursorArticle.getId(), newerArticle.getId());
    }

    @Test
    @DisplayName("기사 개수 조회 성공 - 삭제 기사를 제외하고 조건에 맞는 기사만 집계한다")
    void countArticles_success() {
        // Given
        LocalDateTime base = LocalDateTime.of(2026, 7, 6, 10, 0);

        saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000131"),
                ArticleSource.NAVER,
                "https://news.test/count-1",
                "AI 카운트 기사 1",
                "AI 요약",
                base,
                base,
                1,
                10,
                null
        );

        saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000132"),
                ArticleSource.NAVER,
                "https://news.test/count-2",
                "AI 카운트 기사 2",
                "AI 요약",
                base.plusDays(1),
                base.plusDays(1),
                1,
                10,
                null
        );

        saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000133"),
                ArticleSource.NAVER,
                "https://news.test/count-deleted",
                "AI 삭제 기사",
                "AI 요약",
                base.plusDays(2),
                base.plusDays(2),
                1,
                10,
                base.plusDays(3)
        );

        saveArticle(
                fixedUuid("00000000-0000-0000-0000-000000000134"),
                ArticleSource.CHOSUN,
                "https://news.test/count-other-source",
                "AI 다른 출처 기사",
                "AI 요약",
                base.plusDays(3),
                base.plusDays(3),
                1,
                10,
                null
        );

        entityManager.flush();
        entityManager.clear();

        ArticleSearchCondition condition = new ArticleSearchCondition(
                "AI",
                null,
                List.of(ArticleSource.NAVER),
                base.minusDays(1),
                base.plusDays(5),
                "publishDate",
                "DESC",
                null,
                null,
                10
        );

        // When
        long result = articleQueryRepository.countArticles(condition);

        // Then
        assertThat(result).isEqualTo(2L);
    }

    private User saveUser(String email, String nickname) {
        User user = new User(email, nickname, "password");
        entityManager.persist(user);
        return user;
    }

    private Interest saveInterest(String name) {
        Interest interest = new Interest(name);
        entityManager.persist(interest);
        return interest;
    }

    private Article saveArticle(
            UUID id,
            ArticleSource source,
            String sourceUrl,
            String title,
            String summary,
            LocalDateTime publishDate,
            LocalDateTime createdAt,
            long commentCount,
            long viewCount,
            LocalDateTime deletedAt
    ) {
        Article article = Article.fromBackup(new ArticleBackupDto(
                source,
                sourceUrl,
                title,
                publishDate,
                summary,
                commentCount,
                viewCount,
                createdAt,
                createdAt,
                deletedAt
        ));

        ReflectionTestUtils.setField(article, "id", id);

        entityManager.persist(article);
        entityManager.flush();

        entityManager.createNativeQuery("""
                update articles
                set created_at = :createdAt,
                    updated_at = :createdAt
                where id = :id
                """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate();

        entityManager.flush();

        return article;
    }

    private void saveArticleView(Article article, User user) {
        ArticleView articleView = new ArticleView(article, user);
        entityManager.persist(articleView);
    }

    private void saveArticleInterest(Article article, Interest interest) {
        ArticleInterest articleInterest = new ArticleInterest();

        ArticleInterestId id = new ArticleInterestId();
        ReflectionTestUtils.setField(id, "articleId", article.getId());
        ReflectionTestUtils.setField(id, "interestId", interest.getId());

        ReflectionTestUtils.setField(articleInterest, "id", id);
        ReflectionTestUtils.setField(articleInterest, "article", article);
        ReflectionTestUtils.setField(articleInterest, "interest", interest);

        entityManager.persist(articleInterest);
    }

    private UUID fixedUuid(String value) {
        return UUID.fromString(value);
    }
}