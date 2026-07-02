package com.monew.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 정보 수정 요청 — 닉네임만 수정 가능.
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 20) String nickname
) {
}
