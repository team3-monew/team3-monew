package com.monew.server.user.controller;

import com.monew.server.user.dto.UserDto;
import com.monew.server.user.dto.UserLoginRequest;
import com.monew.server.user.dto.UserRegisterRequest;
import com.monew.server.user.dto.UserUpdateRequest;
import com.monew.server.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegisterRequest request) {
        UserDto created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // 사용자 정보 수정 (닉네임)
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDto> update(
            @PathVariable UUID userId,
            @RequestHeader("MoNew-Request-User-ID") UUID requesterId,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateNickname(userId, requesterId, request));
    }

    // 사용자 논리 삭제
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID userId,
            @RequestHeader("MoNew-Request-User-ID") UUID requesterId) {
        userService.delete(userId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
