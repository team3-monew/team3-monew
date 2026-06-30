package com.monew.server.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 @WebMvcTest 공통 베이스.
 *
 * 전역 AuthInterceptor 가 UserRepository 에 의존하므로, 컨트롤러 슬라이스 테스트에선
 * 이 의존성을 mock 해야 컨텍스트가 로딩된다. 이 베이스가 그 mock 과 기본 인증 스텁을 제공
 *
 * 사용법:
 *   @WebMvcTest(XxxController.class)
 *   class XxxControllerTest extends ControllerTestSupport {
 *       @MockitoBean XxxService xxxService;
 *       ... // 테스트 요청 시 헤더에 MoNew-Request-User-ID 추가
 *   }
 *
 * 미인증(401) 케이스를 검증하려면 테스트 안에서 스텁을 덮어쓰면 된다:
 *   given(userRepository.existsByIdAndDeletedAtIsNull(any())).willReturn(false);
 */
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // 전역 AuthInterceptor 의존성 — 기본적으로 "유효한 로그인 사용자"로 통과
    @MockitoBean
    protected UserRepository userRepository;

    @BeforeEach
    void authStubbing() {
        given(userRepository.existsByIdAndDeletedAtIsNull(any())).willReturn(true);
    }
}
