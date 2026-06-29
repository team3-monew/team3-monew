package com.monew.server.common.security;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import com.monew.server.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 전역 인증 인터셉터.
 * 로그인/회원가입을 제외한 모든 요청에서 MoNew-Request-User-ID 헤더를 검증한다.
 * - 헤더 존재 + UUID 형식 + DB에 활성 사용자 존재 여부 확인
 * - 통과 시 userId 를 request attribute 에 저장해 컨트롤러에서 사용할 수 있게 한다.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_HEADER = "MoNew-Request-User-ID";
    public static final String USER_ID_ATTRIBUTE = "userId";

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        UUID userId;
        try {
            userId = UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }

        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        return true;
    }
}
