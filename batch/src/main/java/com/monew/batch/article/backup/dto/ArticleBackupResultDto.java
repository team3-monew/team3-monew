package com.monew.batch.article.backup.dto;

public record ArticleBackupResultDto(
    long targetCount,   //  백업 대상 기사 개수
    long successCount,    // 성공 개수
    long failureCount,    // 실패 개수
    long fileCount,   // 업로드한 파일 수
    long fileSizeBytes    // 백업 파일 사이즈
) {
}
