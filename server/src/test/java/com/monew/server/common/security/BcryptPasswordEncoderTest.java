package com.monew.server.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link BcryptPasswordEncoder} 단위 테스트.
 *
 * <p>순수 BCrypt(favre) 구현이 (1) 원문을 그대로 저장하지 않고,
 * (2) 검증에 성공/실패를 올바르게 판정하며, (3) salt 로 인해 매 해시가 달라도
 * 모두 검증 가능한지 확인
 */
class BcryptPasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BcryptPasswordEncoder();

    @Test
    @DisplayName("encode - 원문과 다른 해시를 만들고, 그 해시는 matches 로 검증된다")
    void encodeAndMatch() {
        String raw = "monew1!";

        String encoded = passwordEncoder.encode(raw);

        assertThat(encoded).isNotEqualTo(raw);          // 평문 저장 금지
        assertThat(encoded).startsWith("$2");            // BCrypt 해시 포맷
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }

    @Test
    @DisplayName("matches - 비밀번호가 틀리면 false")
    void matches_wrongPassword() {
        String encoded = passwordEncoder.encode("monew1!");

        assertThat(passwordEncoder.matches("wrong-pw", encoded)).isFalse();
    }

    @Test
    @DisplayName("encode - 같은 비밀번호라도 salt 때문에 해시는 매번 다르지만 둘 다 검증된다")
    void encode_producesDifferentHashesButBothVerify() {
        String raw = "monew1!";

        String encoded1 = passwordEncoder.encode(raw);
        String encoded2 = passwordEncoder.encode(raw);

        assertThat(encoded1).isNotEqualTo(encoded2);     // salt 로 해시가 달라짐
        assertThat(passwordEncoder.matches(raw, encoded1)).isTrue();
        assertThat(passwordEncoder.matches(raw, encoded2)).isTrue();
    }
}
