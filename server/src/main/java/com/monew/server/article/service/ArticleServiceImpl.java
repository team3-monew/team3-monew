package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleListQueryResult;
import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.entity.ArticleView;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.article.repository.ArticleViewRepository;
import com.monew.server.article.repository.querydsl.ArticleQueryRepository;
import com.monew.server.article.type.ArticleSortType;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.article.ArticleException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {

    private static final int MAX_LIMIT = 50;

    private final ArticleRepository articleRepository;
    private final ArticleViewRepository articleViewRepository;
    private final ArticleQueryRepository articleQueryRepository;
    private final UserRepository userRepository;

    @Override
    public CursorPageResponse<ArticleResponse> findArticles(
            ArticleSearchCondition condition,
            UUID userId
    ) {
        validateSearchCondition(condition);
        findActiveUser(userId);

        List<ArticleListQueryResult> queriedArticles =
                articleQueryRepository.findArticles(condition, userId);

        boolean hasNext = queriedArticles.size() > condition.limit();

        List<ArticleListQueryResult> pageResults = hasNext
                ? queriedArticles.subList(0, condition.limit())
                : queriedArticles;

        List<ArticleResponse> content = pageResults.stream()
                .map(ArticleListQueryResult::article)
                .toList();

        String nextCursor = null;
        LocalDateTime nextAfter = null;

        if (hasNext && !pageResults.isEmpty()) {
            ArticleListQueryResult lastResult = pageResults.get(pageResults.size() - 1);
            ArticleResponse lastArticle = lastResult.article();

            nextCursor = resolveNextCursor(lastArticle, condition.orderBy());
            nextAfter = lastResult.createdAt();
        }

        long totalElements = articleQueryRepository.countArticles(condition);

        return new CursorPageResponse<>(
                content,
                nextCursor,
                nextAfter,
                content.size(),
                totalElements,
                hasNext
        );
    }

    @Override
    public ArticleResponse findArticle(UUID articleId, UUID userId) {
        findActiveUser(userId);

        Article article = findActiveArticle(articleId);
        boolean viewedByMe = articleViewRepository.existsByArticleIdAndUserId(articleId, userId);

        return ArticleResponse.from(article, viewedByMe);
    }

    @Override
    @Transactional
    public ArticleViewResponse registerArticleView(UUID articleId, UUID userId) {
        validateActiveArticleExists(articleId);
        User user = findActiveUser(userId);

        int inserted = articleViewRepository.insertIgnore(
                UUID.randomUUID(),
                articleId,
                user.getId()
        );

        if (inserted == 1) {
            int updated = articleRepository.increaseViewCount(articleId);

            if (updated != 1) {
                ArticleException exception =
                        new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
                exception.addDetail("articleId", articleId);
                throw exception;
            }
        }

        ArticleView articleView = articleViewRepository
                .findByArticleIdAndUserId(articleId, user.getId())
                .orElseThrow(() -> {
                    ArticleException exception =
                            new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
                    exception.addDetail("articleId", articleId);
                    exception.addDetail("userId", user.getId());
                    return exception;
                });

        return ArticleViewResponse.from(articleView);
    }

    @Override
    @Transactional
    public void softDelete(UUID articleId) {
        Article article = findActiveArticleForUpdate(articleId);
        article.softDelete();
    }

    @Override
    @Transactional
    public void hardDelete(UUID articleId) {
        Article article = findArticleIncludingDeleted(articleId);
        articleRepository.delete(article);
    }

    @Override
    public List<ArticleSource> findSources() {
        return Arrays.asList(ArticleSource.values());
    }

    private void validateSearchCondition(ArticleSearchCondition condition) {
        if (condition == null) {
            throwInvalidRequest("condition", null);
        }

        validateOrderBy(condition.orderBy());
        validateDirection(condition.direction());
        validateLimit(condition.limit());
        validatePublishDateRange(condition.publishDateFrom(), condition.publishDateTo());
        validateCursorPair(condition.cursor(), condition.after());
        validateCursorFormat(condition);
    }

    private void validateOrderBy(String orderBy) {
        if (ArticleSortType.isValid(orderBy)) {
            return;
        }

        throwInvalidRequest("orderBy", orderBy);
    }

    private void validateDirection(String direction) {
        if ("ASC".equalsIgnoreCase(direction) || "DESC".equalsIgnoreCase(direction)) {
            return;
        }

        throwInvalidRequest("direction", direction);
    }

    private void validateLimit(int limit) {
        if (limit <= 0 || limit > MAX_LIMIT) {
            throwInvalidRequest("limit", limit);
        }
    }

    private void validatePublishDateRange(
            LocalDateTime publishDateFrom,
            LocalDateTime publishDateTo
    ) {
        if (publishDateFrom == null || publishDateTo == null) {
            return;
        }

        if (publishDateFrom.isAfter(publishDateTo)) {
            ArticleException exception =
                    new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
            exception.addDetail("publishDateFrom", publishDateFrom);
            exception.addDetail("publishDateTo", publishDateTo);
            throw exception;
        }
    }

    private void validateCursorPair(String cursor, LocalDateTime after) {
        boolean hasCursor = cursor != null && !cursor.isBlank();
        boolean hasAfter = after != null;

        if (hasCursor != hasAfter) {
            ArticleException exception =
                    new ArticleException(ArticleErrorCode.INVALID_ARTICLE_CURSOR);
            exception.addDetail("cursor", cursor);
            exception.addDetail("after", after);
            throw exception;
        }
    }

    private void validateCursorFormat(ArticleSearchCondition condition) {
        String cursor = condition.cursor();

        if (cursor == null || cursor.isBlank()) {
            return;
        }

        try {
            switch (ArticleSortType.from(condition.orderBy())) {
                case COMMENT_COUNT, VIEW_COUNT -> Long.parseLong(cursor);
                case PUBLISH_DATE -> LocalDateTime.parse(cursor);
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            ArticleException exception =
                    new ArticleException(ArticleErrorCode.INVALID_ARTICLE_CURSOR, e);
            exception.addDetail("cursor", cursor);
            exception.addDetail("orderBy", condition.orderBy());
            throw exception;
        }
    }

    private String resolveNextCursor(ArticleResponse article, String orderBy) {
        return switch (ArticleSortType.from(orderBy)) {
            case COMMENT_COUNT -> String.valueOf(article.commentCount());
            case VIEW_COUNT -> String.valueOf(article.viewCount());
            case PUBLISH_DATE -> article.publishDate().toString();
        };
    }

    private void validateActiveArticleExists(UUID articleId) {
        if (articleId == null || !articleRepository.existsByIdAndDeletedAtIsNull(articleId)) {
            ArticleException exception =
                    new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
            exception.addDetail("articleId", articleId);
            throw exception;
        }
    }

    private Article findActiveArticleForUpdate(UUID articleId) {
        return articleRepository.findActiveByIdForUpdate(articleId)
                .orElseThrow(() -> {
                    ArticleException exception =
                            new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
                    exception.addDetail("articleId", articleId);
                    return exception;
                });
    }

    private User findActiveUser(UUID userId) {
        if (userId == null) {
            ArticleException exception =
                    new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
            exception.addDetail("userId", null);
            throw exception;
        }

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> {
                    ArticleException exception =
                            new ArticleException(UserErrorCode.USER_NOT_FOUND);
                    exception.addDetail("userId", userId);
                    return exception;
                });
    }

    private void throwInvalidRequest(String fieldName, Object value) {
        ArticleException exception =
                new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
        exception.addDetail(fieldName, value);
        throw exception;
    }

    private Article findActiveArticle(UUID articleId) {
        return articleRepository.findByIdAndDeletedAtIsNull(articleId)
                .orElseThrow(() -> {
                    ArticleException exception =
                            new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
                    exception.addDetail("articleId", articleId);
                    return exception;
                });
    }

    private Article findArticleIncludingDeleted(UUID articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> {
                    ArticleException exception =
                            new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
                    exception.addDetail("articleId", articleId);
                    return exception;
                });
    }
}