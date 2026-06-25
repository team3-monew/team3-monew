// 예시 템플릿입니다.
// 현재 요구사항/Swagger 기준으로는 Subscription 도메인 기본 ErrorCode를 확정하지 않았기 때문에 전체 주석 처리했습니다.
// Subscription 도메인에서 커스텀 예외가 필요하면 아래 예시를 참고해서 주석을 해제하고 실제 ErrorCode를 정의하세요.

// package com.monew.server.common.exception.subscription;
//
// import com.monew.server.common.exception.ErrorCode;
// import lombok.Getter;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpStatus;
//
// @Getter
// @RequiredArgsConstructor
// public enum SubscriptionErrorCode implements ErrorCode {
//
//     SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다.");
//
//     private final HttpStatus status;
//     private final String message;
// }