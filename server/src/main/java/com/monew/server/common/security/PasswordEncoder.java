package com.monew.server.common.security;

/**
 * 비밀번호 해싱/검증 추상화.
 * 현재 구현은 순수 BCrypt 라이브러리(favre). 추후 Spring Security 도입 시 구현체만 교체하면 됨.
 */
public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
