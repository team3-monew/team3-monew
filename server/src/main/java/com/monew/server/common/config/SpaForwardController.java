package com.monew.server.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA(React Router) 지원 컨트롤러.
 *
 * <p>서버가 직접 처리하지 않는 경로(= 클라이언트 라우팅 경로)를 {@code index.html} 로 forward 한다.
 * 이렇게 해야 사용자가 {@code /interests} 같은 딥링크로 접속하거나 새로고침해도 404 가 나지 않는다.
 *
 * <p>주의: {@code /api/**}, {@code /actuator/**}, 확장자 있는 정적 파일(.js/.css/.woff2 등)은
 * 각자 더 구체적인 핸들러(REST 컨트롤러/Actuator/정적리소스 핸들러)가 우선 처리하므로 여기로 오지 않는다.
 * (아래 패턴은 "마지막 경로 세그먼트에 점(.)이 없는" 경우만 매칭 → 정적 파일 제외)
 */
@Controller
public class SpaForwardController {

    @RequestMapping(value = {"/", "/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
