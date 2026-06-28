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
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.article.ArticleException;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {

    private static final String ORDER_BY_PUBLISH_DATE = "publishDate";
    private static final String ORDER_BY_COMMENT_COUNT = "commentCount";
    private static final String ORDER_BY_VIEW_COUNT = "viewCount";

    private static final String DIRECTION_ASC = "ASC";
    private static final String DIRECTION_DESC = "DESC";

    private static final int MAX_LIMIT = 100;

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
    @Transactional
    public ArticleViewResponse registerArticleView(UUID articleId, UUID userId) {
        Article article = findActiveArticleForUpdate(articleId);
        User user = findActiveUser(userId);

        return articleViewRepository.findByArticleIdAndUserId(articleId, userId)
                .map(this::toArticleViewResponse)
                .orElseGet(() -> createArticleView(article, user));
    }

    @Override
    @Transactional
    public void softDelete(UUID articleId) {
        Article article = findActiveArticleForUpdate(articleId);
        article.softDelete();
    }

    @Override
    public List<ArticleSource> findSources() {
        return Arrays.asList(ArticleSource.values());
    }

    @Override
    public ArticleResponse findArticle(UUID articleId, UUID userId) {
        Article article = findActiveArticle(articleId);
        boolean viewedByMe = articleViewRepository.existsByArticleIdAndUserId(articleId, userId);

        return toArticleResponse(article, viewedByMe);
    }

    @Override
    @Transactional
    public void hardDelete(UUID articleId) {
        Article article = findArticleIncludingDeleted(articleId);
        articleRepository.delete(article);
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
        if (ORDER_BY_PUBLISH_DATE.equals(orderBy)
                || ORDER_BY_COMMENT_COUNT.equals(orderBy)
                || ORDER_BY_VIEW_COUNT.equals(orderBy)) {
            return;
        }

        throwInvalidRequest("orderBy", orderBy);
    }

    private void validateDirection(String direction) {
        if (DIRECTION_ASC.equalsIgnoreCase(direction)
                || DIRECTION_DESC.equalsIgnoreCase(direction)) {
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
            switch (condition.orderBy()) {
                case ORDER_BY_COMMENT_COUNT, ORDER_BY_VIEW_COUNT -> Long.parseLong(cursor);
                case ORDER_BY_PUBLISH_DATE -> LocalDateTime.parse(cursor);
                default -> throwInvalidRequest("orderBy", condition.orderBy());
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
        return switch (orderBy) {
            case ORDER_BY_COMMENT_COUNT -> String.valueOf(article.commentCount());
            case ORDER_BY_VIEW_COUNT -> String.valueOf(article.viewCount());
            case ORDER_BY_PUBLISH_DATE -> article.publishDate().toString();
            default -> {
                ArticleException exception =
                        new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
                exception.addDetail("orderBy", orderBy);
                throw exception;
            }
        };
    }

    private ArticleViewResponse createArticleView(Article article, User user) {
        try {
            ArticleView articleView = new ArticleView(article, user);
            ArticleView savedArticleView = articleViewRepository.saveAndFlush(articleView);

            article.increaseViewCount();

            return toArticleViewResponse(savedArticleView);
        } catch (DataIntegrityViolationException e) {
            ArticleView existingArticleView = articleViewRepository
                    .findByArticleIdAndUserId(article.getId(), user.getId())
                    .orElseThrow(() -> e);

            return toArticleViewResponse(existingArticleView);
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
                            new ArticleException(ArticleErrorCode.INVALID_ARTICLE_REQUEST);
                    exception.addDetail("userId", userId);
                    return exception;
                });
    }

    private ArticleViewResponse toArticleViewResponse(ArticleView articleView) {
        Article article = articleView.getArticle();

        return new ArticleViewResponse(
                articleView.getId(),
                articleView.getUser().getId(),
                articleView.getCreatedAt(),
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishDate(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount()
        );
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

    private ArticleResponse toArticleResponse(Article article, boolean viewedByMe) {
        return new ArticleResponse(
                article.getId(),
                article.getSource(),
                article.getSourceUrl(),
                article.getTitle(),
                article.getPublishDate(),
                article.getSummary(),
                article.getCommentCount(),
                article.getViewCount(),
                viewedByMe
        );
    }

}