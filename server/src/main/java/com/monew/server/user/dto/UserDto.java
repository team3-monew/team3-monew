package com.monew.server.user.dto;

import com.monew.server.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;


// 사용자 응답 DTO. 비밀번호는 절대 포함하지 않는다.

public record UserDto(
        UUID id,
        String email,
        String nickname,
        LocalDateTime createdAt
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCreatedAt()
        );
    }
}
