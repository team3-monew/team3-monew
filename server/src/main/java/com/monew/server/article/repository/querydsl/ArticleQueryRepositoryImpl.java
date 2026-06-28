package com.monew.server.article.repository.querydsl;

import static com.monew.server.article.entity.QArticle.article;
import static com.monew.server.article.entity.QArticleInterest.articleInterest;
import static com.monew.server.article.entity.QArticleView.articleView;

import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.entity.ArticleSource;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArticleQueryRepositoryImpl implements ArticleQueryRepository {

    private static final String ORDER_BY_PUBLISH_DATE = "publishDate";
    private static final String ORDER_BY_COMMENT_COUNT = "commentCount";
    private static final String ORDER_BY_VIEW_COUNT = "viewCount";
    private static final String DIRECTION_ASC = "ASC";

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ArticleListQueryResult> findArticles(
            ArticleSearchCondition condition,
            UUID userId
    ) {
        return queryFactory
                .select(Projections.constructor(
                        ArticleListQueryResult.class,
                        Projections.constructor(
                                ArticleResponse.class,
                                article.id,
                                article.source,
                                article.sourceUrl,
                                article.title,
                                article.publishDate,
                                article.summary,
                                article.commentCount,
                                article.viewCount,
                                articleView.id.isNotNull()
                        ),
                        article.createdAt
                ))
                .from(article)
                .leftJoin(articleView).on(
                        articleView.article.id.eq(article.id),
                        articleView.user.id.eq(userId)
                )
                .where(
                        article.deletedAt.isNull(),
                        keywordContains(condition.keyword()),
                        interestEq(condition.interestId()),
                        sourceIn(condition.sourceIn()),
                        publishDateGoe(condition.publishDateFrom()),
                        publishDateLoe(condition.publishDateTo()),
                        cursorCondition(condition)
                )
                .orderBy(orderSpecifiers(condition))
                .limit(condition.limit() + 1L)
                .fetch();
    }

    @Override
    public long countArticles(ArticleSearchCondition condition) {
        Long count = queryFactory
                .select(article.count())
                .from(article)
                .where(
                        article.deletedAt.isNull(),
                        keywordContains(condition.keyword()),
                        interestEq(condition.interestId()),
                        sourceIn(condition.sourceIn()),
                        publishDateGoe(condition.publishDateFrom()),
                        publishDateLoe(condition.publishDateTo())
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return article.title.containsIgnoreCase(keyword)
                .or(article.summary.containsIgnoreCase(keyword));
    }

    private BooleanExpression interestEq(UUID interestId) {
        if (interestId == null) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(articleInterest)
                .where(
                        articleInterest.article.id.eq(article.id),
                        articleInterest.interest.id.eq(interestId)
                )
                .exists();
    }

    private BooleanExpression sourceIn(List<ArticleSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        return article.source.in(sources);
    }

    private BooleanExpression publishDateGoe(LocalDateTime publishDateFrom) {
        if (publishDateFrom == null) {
            return null;
        }

        return article.publishDate.goe(publishDateFrom);
    }

    private BooleanExpression publishDateLoe(LocalDateTime publishDateTo) {
        if (publishDateTo == null) {
            return null;
        }

        return article.publishDate.loe(publishDateTo);
    }

    private BooleanExpression cursorCondition(ArticleSearchCondition condition) {
        if (condition.cursor() == null || condition.cursor().isBlank()) {
            return null;
        }

        String orderBy = normalizeOrderBy(condition.orderBy());
        boolean asc = isAsc(condition.direction());

        return switch (orderBy) {
            case ORDER_BY_COMMENT_COUNT -> numberCursorCondition(
                    article.commentCount,
                    Long.parseLong(condition.cursor()),
                    condition.after(),
                    asc
            );
            case ORDER_BY_VIEW_COUNT -> numberCursorCondition(
                    article.viewCount,
                    Long.parseLong(condition.cursor()),
                    condition.after(),
                    asc
            );
            case ORDER_BY_PUBLISH_DATE -> dateTimeCursorCondition(
                    article.publishDate,
                    LocalDateTime.parse(condition.cursor()),
                    condition.after(),
                    asc
            );
            default -> dateTimeCursorCondition(
                    article.publishDate,
                    LocalDateTime.parse(condition.cursor()),
                    condition.after(),
                    asc
            );
        };
    }

    private BooleanExpression numberCursorCondition(
            NumberPath<Long> path,
            Long cursor,
            LocalDateTime after,
            boolean asc
    ) {
        if (after == null) {
            return asc ? path.gt(cursor) : path.lt(cursor);
        }

        if (asc) {
            return path.gt(cursor)
                    .or(path.eq(cursor).and(article.createdAt.gt(after)));
        }

        return path.lt(cursor)
                .or(path.eq(cursor).and(article.createdAt.lt(after)));
    }

    private BooleanExpression dateTimeCursorCondition(
            DateTimePath<LocalDateTime> path,
            LocalDateTime cursor,
            LocalDateTime after,
            boolean asc
    ) {
        if (after == null) {
            return asc ? path.gt(cursor) : path.lt(cursor);
        }

        if (asc) {
            return path.gt(cursor)
                    .or(path.eq(cursor).and(article.createdAt.gt(after)));
        }

        return path.lt(cursor)
                .or(path.eq(cursor).and(article.createdAt.lt(after)));
    }

    private OrderSpecifier<?>[] orderSpecifiers(ArticleSearchCondition condition) {
        String orderBy = normalizeOrderBy(condition.orderBy());
        boolean asc = isAsc(condition.direction());

        return switch (orderBy) {
            case ORDER_BY_COMMENT_COUNT -> new OrderSpecifier<?>[]{
                    asc ? article.commentCount.asc() : article.commentCount.desc(),
                    asc ? article.createdAt.asc() : article.createdAt.desc()
            };
            case ORDER_BY_VIEW_COUNT -> new OrderSpecifier<?>[]{
                    asc ? article.viewCount.asc() : article.viewCount.desc(),
                    asc ? article.createdAt.asc() : article.createdAt.desc()
            };
            case ORDER_BY_PUBLISH_DATE -> new OrderSpecifier<?>[]{
                    asc ? article.publishDate.asc() : article.publishDate.desc(),
                    asc ? article.createdAt.asc() : article.createdAt.desc()
            };
            default -> new OrderSpecifier<?>[]{
                    article.publishDate.desc(),
                    article.createdAt.desc()
            };
        };
    }

    private String normalizeOrderBy(String orderBy) {
        if (orderBy == null || orderBy.isBlank()) {
            return ORDER_BY_PUBLISH_DATE;
        }

        return orderBy;
    }

    private boolean isAsc(String direction) {
        return DIRECTION_ASC.equalsIgnoreCase(direction);
    }
}