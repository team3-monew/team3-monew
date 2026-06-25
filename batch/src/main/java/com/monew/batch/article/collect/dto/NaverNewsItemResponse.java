package com.monew.batch.article.collect.dto;

public record NaverNewsItemResponse(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) { }
