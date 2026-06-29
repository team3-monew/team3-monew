package com.monew.batch.article.collect.dto;

public record NaverNewsItemResponseDto(
    String title, // 뉴스 기사의 제목
    String originallink,  // 뉴스 기사 원문의 URL
    String link,  // 뉴스 기사의 네이버 뉴스 URL. 네이버에 제공되지 않은 기사라면 기사 원문의 URL을 반환
    String description, // 뉴스 기사의 내용을 요약한 패시지 정보
    String pubDate  // 뉴스 기사가 네이버에 제공된 시간
) {

}
