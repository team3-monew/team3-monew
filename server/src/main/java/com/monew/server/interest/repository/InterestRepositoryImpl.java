package com.monew.server.interest.repository;

import static com.monew.server.interest.entity.QInterest.interest;
import static com.monew.server.interest.entity.QInterestKeyword.interestKeyword;

import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.QInterest;
import com.monew.server.interest.entity.QInterestKeyword;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class InterestRepositoryImpl implements InterestRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    private final QInterest interest = QInterest.interest;
    private final QInterestKeyword interestKeyword = QInterestKeyword.interestKeyword;

    @Override
    public List<Interest> searchInterests(
            String keyword,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit
    ) {
        return queryFactory
                .selectFrom(interest)
                .where(keywordContains(keyword),
                        cursorCondition(orderBy, direction, cursor, after))
                .orderBy(orderSpecifier(orderBy, direction))
                .limit(limit)
                .fetch();
    }

    @Override
    public long countInterests(String keyword) {
        Long count = queryFactory
                .select(interest.count())
                .from(interest)
                .where(keywordContains(keyword))
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmedKeyword = keyword.trim();

        return interest.name.containsIgnoreCase(trimmedKeyword)
                .or(JPAExpressions
                        .selectOne()
                        .from(interestKeyword)
                        .where(
                                interestKeyword.interest.eq(interest),
                                interestKeyword.keyword.containsIgnoreCase(trimmedKeyword)
                        )
                        .exists());
    }

    private BooleanExpression cursorCondition(
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after) {
        if (!StringUtils.hasText(cursor) || after == null) {
            return null;
        }

        boolean asc = direction.equals("ASC");

        if (orderBy.equals("name")) {
            if (asc) {
                return interest.name.gt(cursor)
                        .or(interest.name.eq(cursor).and(interest.createdAt.gt(after)));
            }
            return interest.name.lt(cursor)
                    .or(interest.name.eq(cursor).and(interest.createdAt.lt(after)));
        }

        long cursorSubscriberCount = Long.parseLong(cursor);

        if (asc) {
            return interest.subscriberCount.gt(cursorSubscriberCount)
                    .or(interest.subscriberCount.eq(cursorSubscriberCount)
                            .and(interest.createdAt.gt(after)));
        }

        return interest.subscriberCount.lt(cursorSubscriberCount)
                .or(interest.subscriberCount.eq(cursorSubscriberCount)
                        .and(interest.createdAt.lt(after)));
    }

    private OrderSpecifier<?>[] orderSpecifier(String orderBy, String direction) {
        Order order = direction.equals("ASC") ? Order.ASC : Order.DESC;

        OrderSpecifier<?> primaryOrder = orderBy.equals("name")
                ? new OrderSpecifier<>(order, interest.name)
                : new OrderSpecifier<>(order, interest.subscriberCount);

        OrderSpecifier<?> createdAtOrder = new OrderSpecifier<>(order, interest.createdAt);
        OrderSpecifier<?> idOrder = new OrderSpecifier<>(order, interest.id);

        return new OrderSpecifier<?>[] {primaryOrder, createdAtOrder, idOrder};
    }
}
