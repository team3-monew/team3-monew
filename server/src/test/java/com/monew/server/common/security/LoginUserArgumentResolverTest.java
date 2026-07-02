package com.monew.server.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@link LoginUserArgumentResolver} 단위 테스트.
 *
 * <p>이 리졸버는 컨트롤러의 {@code @LoginUser UUID} 파라미터에,
 * AuthInterceptor 가 request attribute 에 저장해둔 userId 를 주입
 *
 * <ul>
 *   <li>supportsParameter: {@code @LoginUser} + UUID 타입일 때만 true</li>
 *   <li>resolveArgument: attribute 의 userId 반환, 없으면 방어적으로 401</li>
 * </ul>
 *
 * <p>지원 여부 판정을 검증하려면 실제 애너테이션이 붙은 파라미터가 필요하므로,
 * 아래 {@link #sample} 더미 메서드의 파라미터로 {@link MethodParameter} 를 만들어 사용
 */
@ExtendWith(MockitoExtension.class)
class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    @Mock
    NativeWebRequest webRequest;

    // MethodParameter 생성을 위한 더미 메서드 (호출되지 않음)
    // 0: @LoginUser UUID (지원 O) / 1: UUID (애너테이션 없음 → 지원 X) / 2: @LoginUser String (타입 불일치 → 지원 X)
    @SuppressWarnings("unused")
    void sample(@LoginUser UUID loginUser, UUID plainUuid, @LoginUser String wrongType) {
    }

    private MethodParameter param(int index) throws NoSuchMethodException {
        Method method = LoginUserArgumentResolverTest.class
                .getDeclaredMethod("sample", UUID.class, UUID.class, String.class);
        return new MethodParameter(method, index);
    }

    @Test
    @DisplayName("supportsParameter - @LoginUser + UUID 만 지원")
    void supportsParameter() throws Exception {
        assertThat(resolver.supportsParameter(param(0))).isTrue();  // @LoginUser UUID
        assertThat(resolver.supportsParameter(param(1))).isFalse(); // 애너테이션 없는 UUID
        assertThat(resolver.supportsParameter(param(2))).isFalse(); // @LoginUser 지만 String
    }

    @Test
    @DisplayName("resolveArgument - request attribute 의 userId 를 반환")
    void resolveArgument_returnsUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        given(webRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST))
                .willReturn(userId);

        Object result = resolver.resolveArgument(param(0), null, webRequest, null);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("resolveArgument - attribute 가 없으면 401 (인터셉터 통과 후엔 항상 존재해야 하므로 방어적 처리)")
    void resolveArgument_missingAttribute() throws Exception {
        given(webRequest.getAttribute(AuthInterceptor.USER_ID_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST))
                .willReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(param(0), null, webRequest, null))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }
}
