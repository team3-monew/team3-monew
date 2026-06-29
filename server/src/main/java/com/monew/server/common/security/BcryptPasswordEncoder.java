package com.monew.server.common.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 순수 BCrypt(favre) 기반 PasswordEncoder 구현체.
 */
@Component
public class BcryptPasswordEncoder implements PasswordEncoder {

    private static final int COST = 10; // BCrypt 작업 강도 (기본 10)

    @Override
    public String encode(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.verifyer()
                .verify(rawPassword.toCharArray(), encodedPassword)
                .verified;
    }
}
