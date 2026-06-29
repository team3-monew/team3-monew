package com.monew.server.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청.
 * 비밀번호 정책(min/패턴)은 팀/명세와 확정 필요 — 현재는 샘플 계정(monew1!, 7자)을 통과하도록 lenient.
 */
public record UserRegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 20) String nickname,
        @NotBlank @Size(min = 6, max = 64) String password
) {
}
