package com.monew.server.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청별 추적용 MDC 필터.
 * - 요청마다 고유 requestId 생성 + 클라이언트 IP를 MDC에 저장 → 모든 로그에 자동 포함
 * - requestId를 응답 헤더(MoNew-Request-ID)로 내려 클라이언트가 문의 시 추적 가능
 * - 요청 종료 시 MDC.clear() 로 스레드 재사용에 따른 값 누수 방지
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String CLIENT_IP = "clientIp";
    public static final String REQUEST_ID_HEADER = "MoNew-Request-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, resolveClientIp(request));
            response.setHeader(REQUEST_ID_HEADER, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // 반드시 정리 (스레드풀 재사용 시 값 섞임 방지)
        }
    }

    // ALB/프록시 뒤에서는 실제 클라이언트 IP가 X-Forwarded-For 에 담김
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
