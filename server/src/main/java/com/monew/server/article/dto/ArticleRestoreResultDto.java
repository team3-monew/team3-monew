package com.monew.server.article.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleRestoreResultDto(
    Instant restoreDate,    // 복구 날짜
    List<UUID> restoredArticleIds,    // 복구된 뉴스기사 ID 리스트
    long restoredArticleCount   // 복구된 기사 수
) {
}
