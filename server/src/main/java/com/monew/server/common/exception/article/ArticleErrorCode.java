package com.monew.server.common.exception.article;

import com.monew.server.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode implements ErrorCode {

    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "뉴스 기사 정보를 찾을 수 없습니다."),
    INVALID_ARTICLE_REQUEST(HttpStatus.BAD_REQUEST, "뉴스 기사 요청 값이 올바르지 않습니다."),
    INVALID_ARTICLE_CURSOR(HttpStatus.BAD_REQUEST, "뉴스 기사 커서 값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}