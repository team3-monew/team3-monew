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


    return queryFactory
        .selectFrom(comment)
        .join(comment.user, user).fetchJoin()
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
          UUID articleId, Long lastLikeCount, LocalDateTime lastCreatedAt, UUID lastId,
          String direction, Pageable pageable
  ) {
    boolean asc = isAsc(direction);

    return queryFactory
            .selectFrom(comment)
            .join(comment.user, user).fetchJoin()
            .where(
                    comment.article.id.eq(articleId),
                    comment.deletedAt.isNull(),
                    likeCountCursorCondition(lastLikeCount, lastCreatedAt, lastId, asc)
            )
            .orderBy(
                    asc ? comment.likeCount.asc() : comment.likeCount.desc(),
                    // [변경] 동점자는 direction과 무관하게 항상 오래된 댓글이 위
                    comment.createdAt.asc(),
                    comment.id.asc()
            )
            .limit(pageable.getPageSize())
            .fetch();
  }

  private BooleanExpression createdAtCursorCondition(
          LocalDateTime lastCreatedAt, UUID lastId, boolean asc
  ) {
    if (lastCreatedAt == null) {
      return null;
    }
    return asc
            ? comment.createdAt.gt(lastCreatedAt)
            .or(comment.createdAt.eq(lastCreatedAt).and(comment.id.gt(lastId)))
            : comment.createdAt.lt(lastCreatedAt)
            .or(comment.createdAt.eq(lastCreatedAt).and(comment.id.lt(lastId)));
  }


  private BooleanExpression likeCountCursorCondition(
          Long lastLikeCount, LocalDateTime lastCreatedAt, UUID lastId, boolean asc
  ) {
    if (lastLikeCount == null) {
      return null;
    }

    BooleanExpression likeCountCompare = asc
            ? comment.likeCount.gt(lastLikeCount)
            : comment.likeCount.lt(lastLikeCount);

    // [변경] 동점자 경계를 id가 아니라 createdAt 기준으로 맞춤 (ORDER BY와 동일 기준)
    BooleanExpression tieBreak = comment.createdAt.gt(lastCreatedAt)
            .or(comment.createdAt.eq(lastCreatedAt).and(comment.id.gt(lastId)));

    return likeCountCompare.or(comment.likeCount.eq(lastLikeCount).and(tieBreak));
  }


  private boolean isAsc(String direction) {
    return "ASC".equalsIgnoreCase(direction);
  }
}