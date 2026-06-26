package com.monew.server.user.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.user.dto.UserDto;
import com.monew.server.user.dto.UserLoginRequest;
import com.monew.server.user.dto.UserRegisterRequest;
import com.monew.server.user.entity.User;
import com.monew.server.common.security.PasswordEncoder;
import com.monew.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto register(UserRegisterRequest request) {
        // 이메일 정규화 — 대소문자/공백 우회 가입 방지
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BaseException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User(
                email,
                request.nickname(),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);

        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserDto login(UserLoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // 보안: 이메일 미존재/탈퇴/비번불일치 모두 동일한 예외로 처리 (이메일 존재 여부 유출 방지)
        User user = userRepository.findByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BaseException(UserErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BaseException(UserErrorCode.LOGIN_FAILED);
        }

        return UserDto.from(user);
    }
}
