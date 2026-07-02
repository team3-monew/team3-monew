package com.monew.server.article.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.server.article.dto.ArticleResponse;
import com.monew.server.article.dto.ArticleSearchCondition;
import com.monew.server.article.dto.ArticleViewResponse;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.service.ArticleRestoreService;
import com.monew.server.article.service.ArticleService;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.support.ControllerTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ArticleController.class)
class ArticleControllerTest extends ControllerTestSupport {

    @MockitoBean
    ArticleService articleService;

    // ArticleController 생성자 의존성 때문에 WebMvcTest 컨텍스트 로딩용 mock은 필요하다.
    // 다만 기사 복구 기능 자체는 배치 담당 범위라 여기서는 테스트하지 않는다.
    @MockitoBean
    ArticleRestoreService articleRestoreService;

    @Test
    @DisplayName("기사 목록 조회 성공 - 검색 조건과 로그인 사용자 ID를 서비스로 전달한다")
    void findArticles_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        ArticleResponse article = articleResponse(UUID.randomUUID(), false);
        CursorPageResponse<ArticleResponse> response = new CursorPageResponse<>(
                List.of(article),
                article.publishDate() + "|" + article.id(),
                LocalDateTime.of(2026, 6, 30, 10, 1),
                1,
                1,
                false
        );
        given(articleService.findArticles(any(ArticleSearchCondition.class), eq(userId)))
                .willReturn(response);

        // when
        // then
        mockMvc.perform(get("/api/articles")
                        .header("MoNew-Request-User-ID", userId)
                        .param("keyword", "경제")
                        .param("interestId", interestId.toString())
                        .param("sourceIn", "NAVER", "YEONHAP")
                        .param("publishDateFrom", "2026-06-01T00:00:00")
                        .param("publishDateTo", "2026-06-30T23:59:59")
                        .param("orderBy", "publishDate")
                        .param("direction", "DESC")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(article.id().toString()))
                .andExpect(jsonPath("$.content[0].title").value(article.title()))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));

        then(articleService).should().findArticles(any(ArticleSearchCondition.class), eq(userId));
    }

    @Test
    @DisplayName("기사 단건 조회 성공 - 기사 ID와 로그인 사용자 ID를 서비스로 전달한다")
    void findArticle_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        ArticleResponse response = articleResponse(articleId, true);
        given(articleService.findArticle(articleId, userId)).willReturn(response);

        // when
        // then
        mockMvc.perform(get("/api/articles/{articleId}", articleId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleId.toString()))
                .andExpect(jsonPath("$.viewedByMe").value(true));
    }

    @Test
    @DisplayName("기사 조회 등록 성공 - 조회 응답을 200 OK로 반환한다")
    void registerArticleView_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID articleViewId = UUID.randomUUID();
        ArticleViewResponse response = new ArticleViewResponse(
                articleViewId,
                userId,
                LocalDateTime.of(2026, 6, 30, 10, 5),
                articleId,
                ArticleSource.NAVER,
                "https://news.monew.test/articles/" + articleId,
                "기사 제목",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                "기사 요약",
                2,
                8
        );
        given(articleService.registerArticleView(articleId, userId)).willReturn(response);

        // when
        // then
        mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(articleViewId.toString()))
                .andExpect(jsonPath("$.viewedBy").value(userId.toString()))
                .andExpect(jsonPath("$.articleId").value(articleId.toString()))
                .andExpect(jsonPath("$.articleViewCount").value(8));
    }

    @Test
    @DisplayName("기사 논리 삭제 성공 - 서비스 호출 후 204 No Content를 반환한다")
    void softDelete_success() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(delete("/api/articles/{articleId}", articleId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isNoContent());

        then(articleService).should().softDelete(articleId);
    }

    @Test
    @DisplayName("기사 물리 삭제 성공 - 서비스 호출 후 204 No Content를 반환한다")
    void hardDelete_success() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(delete("/api/articles/{articleId}/hard", articleId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isNoContent());

        then(articleService).should().hardDelete(articleId);
    }

    @Test
    @DisplayName("기사 출처 목록 조회 성공 - 모든 출처 enum 값을 반환한다")
    void findSources_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        given(articleService.findSources()).willReturn(List.of(ArticleSource.NAVER, ArticleSource.YEONHAP));

        // when
        // then
        mockMvc.perform(get("/api/articles/sources")
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("NAVER"))
                .andExpect(jsonPath("$[1]").value("YEONHAP"));
    }

    @Test
    @DisplayName("기사 API 인증 실패 - 인증 헤더가 없으면 인터셉터에서 401을 반환한다")
    void articleApi_fail_missingAuthHeader() throws Exception {
        // given
        UUID articleId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(get("/api/articles/{articleId}", articleId))
                .andExpect(status().isUnauthorized());
    }

    private ArticleResponse articleResponse(UUID articleId, boolean viewedByMe) {
        return new ArticleResponse(
                articleId,
                ArticleSource.NAVER,
                "https://news.monew.test/articles/" + articleId,
                "기사 제목",
                LocalDateTime.of(2026, 6, 30, 10, 0),
                "기사 요약",
                2,
                8,
                viewedByMe
        );
    }
}