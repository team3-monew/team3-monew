package com.monew.server.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import com.monew.server.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 전역 인증 인터셉터({@link AuthInterceptor}) 단위 테스트.
 *
 * <p>인터셉터는 MoNew-Request-User-ID 헤더를 3단계로 검증:
 * (1) 헤더 존재, (2) UUID 형식, (3) DB에 활성(미삭제) 사용자 존재.
 * 어느 단계든 실패하면 401(UNAUTHORIZED), 통과하면 request attribute 에 userId 를 저장
 *
 * <p>MVC 컨텍스트 없이 순수 단위로 검증하기 위해 UserRepository 는 Mockito 로 mock 하고,
 * 요청/응답은 스프링의 MockHttpServletRequest/Response 로 대체
 */
@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AuthInterceptor authInterceptor;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Object handler = new Object(); // preHandle 의 handler 인자 — 검증에 쓰이지 않음

    @Test
    @DisplayName("유효한 헤더 - 통과(true) 및 userId 를 request attribute 에 저장")
    void preHandle_valid() {
        // given: 유효한 UUID 헤더 + DB에 활성 사용자 존재
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthInterceptor.USER_ID_HEADER, userId.toString());
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(true);

        // when
        boolean result = authInterceptor.preHandle(request, response, handler);

        // then: 통과하고, 컨트롤러가 @LoginUser 로 꺼내 쓸 userId 가 저장돼 있어야 한다
        assertThat(result).isTrue();
        assertThat(request.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE)).isEqualTo(userId);
    }

    @Test
    @DisplayName("헤더 없음 - 401 (repository 조회 이전 단계에서 차단)")
    void preHandle_noHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest(); // 헤더 미설정

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("헤더가 공백 문자열 - 401")
    void preHandle_blankHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthInterceptor.USER_ID_HEADER, "   ");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("헤더가 UUID 형식이 아님 - 401")
    void preHandle_invalidUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthInterceptor.USER_ID_HEADER, "not-a-uuid");

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("DB에 활성 사용자 없음(미가입/탈퇴) - 401")
    void preHandle_userNotActive() {
        // given: 형식은 유효하지만 활성 사용자가 아님
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthInterceptor.USER_ID_HEADER, userId.toString());
        given(userRepository.existsByIdAndDeletedAtIsNull(userId)).willReturn(false);

        assertThatThrownBy(() -> authInterceptor.preHandle(request, response, handler))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
