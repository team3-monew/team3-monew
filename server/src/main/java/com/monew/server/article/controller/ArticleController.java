package com.monew.server.article.controller;

import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.service.ArticleService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

    private final ArticleService articleService;

    @PostMapping("/{articleId}/article-views")
    public ResponseEntity<ArticleViewResponse> registerArticleView(
            @PathVariable UUID articleId,
            @RequestHeader(REQUEST_USER_ID_HEADER) UUID userId
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

    @GetMapping("/sources")
    public ResponseEntity<List<ArticleSource>> findSources() {
        return ResponseEntity.ok(articleService.findSources());
    }
}