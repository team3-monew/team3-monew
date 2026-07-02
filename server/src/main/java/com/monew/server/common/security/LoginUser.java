package com.monew.server.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증된 사용자 ID(UUID)를 컨트롤러 파라미터에 주입한다.
 * AuthInterceptor 가 request attribute 에 저장한 값을 LoginUserArgumentResolver 가 꺼내 넣는다.
 *
 * 사용 예: public ResponseEntity<?> foo(@LoginUser UUID userId) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
