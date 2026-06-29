package com.monew.server.article.type;

import java.util.Arrays;

public enum ArticleSortType {

    PUBLISH_DATE("publishDate"),
    COMMENT_COUNT("commentCount"),
    VIEW_COUNT("viewCount");

    private final String value;

    ArticleSortType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isValid(String value) {
        return Arrays.stream(values())
                .anyMatch(type -> type.value.equals(value));
    }

    public static ArticleSortType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid article sort type: " + value)
                );
    }
}