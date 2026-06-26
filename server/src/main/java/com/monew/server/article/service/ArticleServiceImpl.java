package com.monew.server.article.service;

import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.entity.ArticleView;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.article.repository.ArticleViewRepository;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.article.ArticleException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
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

    private final ArticleRepository articleRepository;
    private final ArticleViewRepository articleViewRepository;
    private final UserRepository userRepository;

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
        if (articleId == null) {
            ArticleException exception = new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
            exception.addDetail("articleId", null);
            throw exception;
        }

        return articleRepository.findActiveByIdForUpdate(articleId)
                .orElseThrow(() -> {
                    ArticleException exception = new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
                    exception.addDetail("articleId", articleId);
                    return exception;
                });
    }

    private User findActiveUser(UUID userId) {
        if (userId == null) {
            BaseException exception = new BaseException(UserErrorCode.USER_NOT_FOUND);
            exception.addDetail("userId", null);
            throw exception;
        }

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> {
                    BaseException exception = new BaseException(UserErrorCode.USER_NOT_FOUND);
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
}