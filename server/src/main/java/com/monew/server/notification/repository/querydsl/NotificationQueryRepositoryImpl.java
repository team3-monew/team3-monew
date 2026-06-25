package com.monew.server.notification.repository.querydsl;

import static com.monew.server.notification.entity.QNotification.notification;

import com.monew.server.notification.dto.NotificationResponse;
import com.monew.server.notification.repository.NotificationQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NotificationResponse> findUnreadNotifications(
            UUID userId,
            String cursor,
            LocalDateTime after,
            int limit
    ) {
        return queryFactory
                .select(Projections.constructor(
                        NotificationResponse.class,
                        notification.id,
                        notification.createdAt,
                        notification.confirmedAt,
                        notification.confirmed,
                        notification.user.id,
                        notification.content,
                        notification.resourceType.stringValue().lower(),
                        notification.resourceId
                ))
                .from(notification)
                .where(
                        notification.user.id.eq(userId),
                        notification.confirmed.isFalse(),
                        cursorCondition(cursor, after)
                )
                .orderBy(
                        notification.createdAt.desc(),
                        notification.id.desc()
                )
                .limit(limit + 1)
                .fetch();
    }

    @Override
    public long countUnreadNotifications(UUID userId) {
        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .where(
                        notification.user.id.eq(userId),
                        notification.confirmed.isFalse()
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression cursorCondition(String cursor, LocalDateTime after) {
        if (cursor == null || after == null) {
            return null;
        }

        UUID cursorId = UUID.fromString(cursor);

        return notification.createdAt.lt(after)
                .or(
                        notification.createdAt.eq(after)
                                .and(notification.id.lt(cursorId))
                );
    }
}