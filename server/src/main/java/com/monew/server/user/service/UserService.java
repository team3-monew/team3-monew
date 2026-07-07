package com.monew.server.user.service;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.user.dto.UserDto;
import com.monew.server.user.dto.UserLoginRequest;
import com.monew.server.user.dto.UserRegisterRequest;
import com.monew.server.user.dto.UserUpdateRequest;
import com.monew.server.user.entity.User;
import com.monew.server.common.security.PasswordEncoder;
import com.monew.server.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// event publisher 관련 import
import com.monew.server.activity.event.UserCreatedEvent;
import com.monew.server.activity.event.UserDeletedEvent;
import com.monew.server.activity.event.UserNicknameUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // event publisher용 필드 추가
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserDto register(UserRegisterRequest request) {
        String email = request.email().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BaseException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User(
                email,
                request.nickname(),
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);

        // event publisher 삽입
        eventPublisher.publishEvent(
            new UserCreatedEvent(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getCreatedAt()
            )
        );

        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserDto login(UserLoginRequest request) {
        String email = request.email().trim();

        // 보안: 이메일 미존재/탈퇴/비번불일치 모두 동일한 예외로 처리 (이메일 존재 여부 유출 방지)
        User user = userRepository.findByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BaseException(UserErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BaseException(UserErrorCode.LOGIN_FAILED);
        }

        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateNickname(UUID targetUserId, UUID requesterId, UserUpdateRequest request) {
        validateSelf(targetUserId, requesterId);

        User user = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        user.updateNickname(request.nickname());

        // event publisher 삽입
        eventPublisher.publishEvent(
            new UserNicknameUpdatedEvent(
                user.getId(),
                user.getNickname()
            )
        );

        return UserDto.from(user);
    }

    @Transactional
    public void delete(UUID targetUserId, UUID requesterId) {
        validateSelf(targetUserId, requesterId);

        User user = userRepository.findByIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        user.delete(); // 논리 삭제 — 요구사항상 관련 정보는 '유지'(복구 가능).
        // Mongo 활동내역 삭제는 물리삭제 시에만 (직접 hardDelete / 1일 뒤 배치). 여기선 발행하지 않음.
    }

    @Transactional
    public void hardDelete(UUID targetUserId, UUID requesterId) {
        validateSelf(targetUserId, requesterId);

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

        userRepository.delete(user); // 물리 삭제 (연관 데이터는 DB FK CASCADE)

        // Mongo user_activities 정리 — PG는 FK CASCADE로 지워지지만 Mongo는 이벤트로 삭제해야 함.
        // (논리삭제를 거친 뒤 물리삭제되는 경우엔 이미 삭제돼 있어 멱등)
        eventPublisher.publishEvent(
            new UserDeletedEvent(user.getId())
        );
    }

    // 본인 확인 — 요청자(헤더)와 대상(경로)이 일치해야 함
    private void validateSelf(UUID targetUserId, UUID requesterId) {
        if (!targetUserId.equals(requesterId)) {
            throw new BaseException(UserErrorCode.USER_ACCESS_DENIED);
        }
    }
}
