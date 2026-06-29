package com.monew.server.common.security;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginUser 가 붙은 UUID 파라미터에, AuthInterceptor 가 저장한 userId 를 주입
 * userId 파라미터가 필요한 코드에
 * import com.monew.server.common.security.LoginUser; import 후
 * @LoginUser UUID userId 넣기
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object userId = webRequest.getAttribute(
                AuthInterceptor.USER_ID_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);

        if (userId == null) {
            // 인터셉터가 통과시킨 요청이라면 항상 존재해야 함 (방어적 처리)
            throw new BaseException(CommonErrorCode.UNAUTHORIZED);
        }
        return userId; // AuthInterceptor 가 UUID 로 저장함
    }
}
