package com.monew.server.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 요청별 추적용 MDC 필터({@link MdcLoggingFilter}) 단위 테스트.
 *
 * <p>검증 포인트:
 * <ul>
 *   <li>필터 체인이 도는 동안 MDC 에 requestId·clientIp 가 채워져 있다(→ 모든 로그에 자동 포함)</li>
 *   <li>응답 헤더 MoNew-Request-ID 에 requestId 가 실린다</li>
 *   <li>clientIp 는 ALB/프록시 대비 X-Forwarded-For 를 우선하고, 없으면 remoteAddr 를 쓴다</li>
 *   <li>요청 종료 후 MDC 가 비워진다(스레드풀 재사용 시 값 누수 방지)</li>
 * </ul>
 */
class MdcLoggingFilterTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @Test
    @DisplayName("필터 체인 중 MDC에 requestId·clientIp 저장 + 응답헤더 설정, 종료 후 MDC clear")
    void putsMdcAndResponseHeader_thenClears() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 체인이 실행되는 "그 순간"의 MDC 값을 캡처한다 (필터가 끝나면 clear 되므로)
        String[] captured = new String[2];
        FilterChain chain = (req, res) -> {
            captured[0] = MDC.get(MdcLoggingFilter.REQUEST_ID);
            captured[1] = MDC.get(MdcLoggingFilter.CLIENT_IP);
        };

        filter.doFilter(request, response, chain);

        // 체인 실행 중에는 값이 있었고
        assertThat(captured[0]).isNotBlank();
        assertThat(captured[1]).isEqualTo("9.9.9.9");
        // 응답 헤더에 requestId 가 실렸으며 (체인 중 캡처한 값과 동일)
        assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).isEqualTo(captured[0]);
        // 요청 종료 후에는 MDC 가 비워짐
        assertThat(MDC.get(MdcLoggingFilter.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcLoggingFilter.CLIENT_IP)).isNull();
    }

    @Test
    @DisplayName("clientIp - X-Forwarded-For 가 있으면 그 첫 번째 IP를 우선 사용")
    void clientIp_prefersXForwardedFor() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // 프록시 IP
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8"); // 실제 클라이언트, 중간 프록시
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] clientIp = new String[1];
        FilterChain chain = (req, res) -> clientIp[0] = MDC.get(MdcLoggingFilter.CLIENT_IP);

        filter.doFilter(request, response, chain);

        assertThat(clientIp[0]).isEqualTo("1.2.3.4"); // 맨 앞(실제 클라이언트)
    }
}
