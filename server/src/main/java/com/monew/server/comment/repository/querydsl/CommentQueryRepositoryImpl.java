package com.monew.server.comment.repository.querydsl;

import static com.monew.server.comment.entity.QComment.comment;
import static com.monew.server.user.entity.QUser.user;
import com.monew.server.comment.entity.Comment;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentQueryRepositoryImpl implements CommentQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Comment> findCommentsByArticleValueCursor(
      UUID articleId, LocalDateTime lastCreatedAt, UUID lastId, String direction, Pageable pageable
  ) {
    boolean asc = isAsc(direction);

    // [변경] 기존 CASE WHEN 혼합 정렬 → sortBy별로 별도 쿼리 메서드로 분리
    // (JPQL에서 타입 혼합 파라미터 때문에 발생하던 InvalidDataAccessResourceUsageException 원인 제거)
    return queryFactory
        .selectFrom(comment)
        .join(comment.user, user).fetchJoin() // [유지] N+1 방지
        .where(
            comment.article.id.eq(articleId),
            comment.deletedAt.isNull(),
            createdAtCursorCondition(lastCreatedAt, lastId, asc)
        )
        .orderBy(
            asc ? comment.createdAt.asc() : comment.createdAt.desc(),
            asc ? comment.id.asc() : comment.id.desc() // tie-breaker
        )
        .limit(pageable.getPageSize())
        .fetch();
  }

  @Override
  public List<Comment> findCommentsByArticleLikeCursor(
      UUID articleId, Long lastLikeCount, UUID lastId, String direction, Pageable pageable
  ) {
    boolean asc = isAsc(direction);

    return queryFactory
        .selectFrom(comment)
        .join(comment.user, user).fetchJoin()
        .where(
            comment.article.id.eq(articleId),
            comment.deletedAt.isNull(),
            likeCountCursorCondition(lastLikeCount, lastId, asc)
        )
        .orderBy(
            asc ? comment.likeCount.asc() : comment.likeCount.desc(),
            // [[변경]] 동점자 처리를 id(랜덤 UUID) 대신 createdAt으로 변경
            // → 좋아요 수가 같으면 같은 방향의 날짜순으로 자연스럽게 정렬됨
            asc ? comment.createdAt.asc() : comment.createdAt.desc(),
            comment.id.asc()
        )
        .limit(pageable.getPageSize())
        .fetch();
  }

  // [변경] null이면 where절에서 자동 제외 → 최초 조회(cursor 없음)에서
  // LocalDateTime/Long 타입 추론이 꼬이던 문제가 원천적으로 발생하지 않음
  private BooleanExpression createdAtCursorCondition(LocalDateTime lastCreatedAt, UUID lastId, boolean asc) {
    if (lastCreatedAt == null) {
      return null;
    }
    return asc
        ? comment.createdAt.gt(lastCreatedAt)
        .or(comment.createdAt.eq(lastCreatedAt).and(comment.id.gt(lastId)))
        : comment.createdAt.lt(lastCreatedAt)
            .or(comment.createdAt.eq(lastCreatedAt).and(comment.id.lt(lastId)));
  }

  private BooleanExpression likeCountCursorCondition(Long lastLikeCount, UUID lastId, boolean asc) {
    if (lastLikeCount == null) {
      return null;
    }
    return asc
        ? comment.likeCount.gt(lastLikeCount)
        .or(comment.likeCount.eq(lastLikeCount).and(comment.id.gt(lastId)))
        : comment.likeCount.lt(lastLikeCount)
            .or(comment.likeCount.eq(lastLikeCount).and(comment.id.lt(lastId)));
  }

  private boolean isAsc(String direction) {
    return "ASC".equalsIgnoreCase(direction);
  }
}