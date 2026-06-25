package com.monew.batch.article.collect.dto;

import java.util.List;

public record NaverNewsResponse (
    List<NaverNewsItemResponse> items
) { }
