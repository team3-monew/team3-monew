package com.monew.batch.article.collect.dto;

import java.util.List;

public record NaverNewsResponseDto(
    List<NaverNewsItemResponseDto> items
) {

}
