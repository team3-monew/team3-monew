package com.monew.server.article.controller;

import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.service.ArticleService;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.common.security.LoginUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<ArticleResponse>> findArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID interestId,
            @RequestParam(required = false) List<ArticleSource> sourceIn,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime publishDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime publishDateTo,
            @RequestParam String orderBy,
            @RequestParam String direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime after,
            @RequestParam int limit,
            @LoginUser UUID userId
    ) {
        ArticleSearchCondition condition = new ArticleSearchCondition(
                keyword,
                interestId,
                sourceIn,
                publishDateFrom,
                publishDateTo,
                orderBy,
                direction,
                cursor,
                after,
                limit
        );

        CursorPageResponse<ArticleResponse> response =
                articleService.findArticles(condition, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleResponse> findArticle(
            @PathVariable UUID articleId,
            @LoginUser UUID userId
    ) {
        ArticleResponse response = articleService.findArticle(articleId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{articleId}/article-views")
    public ResponseEntity<ArticleViewResponse> registerArticleView(
            @PathVariable UUID articleId,
            @LoginUser UUID userId
    ) {
        ArticleViewResponse response = articleService.registerArticleView(articleId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> softDelete(
            @PathVariable UUID articleId
    ) {
        articleService.softDelete(articleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{articleId}/hard")
    public ResponseEntity<Void> hardDelete(
            @PathVariable UUID articleId
    ) {
        articleService.hardDelete(articleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sources")
    public ResponseEntity<List<ArticleSource>> findSources() {
        return ResponseEntity.ok(articleService.findSources());
    }
}