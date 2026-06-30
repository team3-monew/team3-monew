package com.monew.server.interest.repository;

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
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class InterestRepositoryImpl implements InterestRepositoryCustom {

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

        // 수정 : cursor에 정렬 필드 값과 마지막 요소 id를 함께 담아 파싱
        ParsedCursor parsedCursor = parseCursor(cursor);
        String cursorValue = parsedCursor.value();
        UUID cursorId = parsedCursor.id();

        boolean asc = direction.equals("ASC");

        if (orderBy.equals("name")) {
            return nameCursorCondition(cursorValue, after, cursorId, asc);
        }

        long cursorSubscriberCount = Long.parseLong(cursorValue);
        return subscriberCountCursorCondition(cursorSubscriberCount, after, cursorId, asc);
    }

    private BooleanExpression nameCursorCondition(
            String cursorName,
            LocalDateTime after,
            UUID cursorId,
            boolean asc
    ) {
        if (asc) {
            return interest.name.gt(cursorName)
                    .or(interest.name.eq(cursorName)
                            .and(interest.createdAt.gt(after)))
                    .or(interest.name.eq(cursorName)
                            .and(interest.createdAt.eq(after))
                            .and(interest.id.gt(cursorId)));
        }

        return interest.name.lt(cursorName)
                .or(interest.name.eq(cursorName)
                        .and(interest.createdAt.lt(after)))
                .or(interest.name.eq(cursorName)
                        .and(interest.createdAt.eq(after))
                        .and(interest.id.lt(cursorId)));
    }

    private BooleanExpression subscriberCountCursorCondition(
            long cursorSubscriberCount,
            LocalDateTime after,
            UUID cursorId,
            boolean asc
    ) {
        if (asc) {
            return interest.subscriberCount.gt(cursorSubscriberCount)
                    .or(interest.subscriberCount.eq(cursorSubscriberCount)
                            .and(interest.createdAt.gt(after)))
                    .or(interest.subscriberCount.eq(cursorSubscriberCount)
                            .and(interest.createdAt.eq(after))
                            .and(interest.id.gt(cursorId)));
        }

        return interest.subscriberCount.lt(cursorSubscriberCount)
                .or(interest.subscriberCount.eq(cursorSubscriberCount)
                        .and(interest.createdAt.lt(after)))
                .or(interest.subscriberCount.eq(cursorSubscriberCount)
                        .and(interest.createdAt.eq(after))
                        .and(interest.id.lt(cursorId)));
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

    // 추가 : nextCursor 형식인 "정렬값|id"를 파싱
    private ParsedCursor parseCursor(String cursor) {
        int delimiterIndex = cursor.lastIndexOf("|");
        String value = cursor.substring(0, delimiterIndex);
        UUID id = UUID.fromString(cursor.substring(delimiterIndex + 1));

        return new ParsedCursor(value, id);
    }

    // PR 수정 : 커서 파싱 결과를 담기 위한 내부 record
    private record ParsedCursor(String value, UUID id) {}
}
