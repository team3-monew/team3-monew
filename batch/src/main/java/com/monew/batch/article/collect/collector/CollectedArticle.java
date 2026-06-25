package com.monew.batch.article.collect.collector;

import com.monew.batch.article.entity.ArticleSource;
import java.time.LocalDateTime;


// 외부 뉴스 api에서 가져온 기사 우리 서비스가 저장하기 좋은 공통 형태로 바꾼 DTO
public record CollectedArticle(
    ArticleSource source,     // 뉴스 출처
    String sourceUrl,     // 뉴스 원본 링크
    String title,     // 뉴스 제목
    LocalDateTime publishDate,    // 뉴스 발행 날짜
    String summary      // 뉴스 내용 요약
) { }
