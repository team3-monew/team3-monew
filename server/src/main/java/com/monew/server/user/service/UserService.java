package com.monew.server.user.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.user.dto.UserDto;
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
}
