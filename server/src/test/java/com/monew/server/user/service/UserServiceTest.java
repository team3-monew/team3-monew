package com.monew.server.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.common.security.PasswordEncoder;
import com.monew.server.user.dto.UserDto;
import com.monew.server.user.dto.UserLoginRequest;
import com.monew.server.user.dto.UserRegisterRequest;
import com.monew.server.user.dto.UserUpdateRequest;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// event publisher 관련 import
import org.springframework.context.ApplicationEventPublisher;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    // event publisher 관련 mock
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("회원가입 성공 - 대소문자 보존 및 비밀번호 해싱")
    void register_success() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("Woody@Monew.com", "우디", "monew1!");
        given(userRepository.existsByEmail("Woody@Monew.com")).willReturn(false);
        given(passwordEncoder.encode("monew1!")).willReturn("hashed-pw");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        UserDto result = userService.register(request);

        // then
        assertThat(result.email()).isEqualTo("Woody@Monew.com"); // 대소문자 보존
        assertThat(result.nickname()).isEqualTo("우디");
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void register_duplicateEmail() {
        // given
        UserRegisterRequest request = new UserRegisterRequest("woody@monew.com", "우디", "monew1!");
        given(userRepository.existsByEmail("woody@monew.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        UserLoginRequest request = new UserLoginRequest("Woody@Monew.com", "monew1!");
        User user = new User("Woody@Monew.com", "우디", "hashed-pw");
        given(userRepository.findByEmail("Woody@Monew.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("monew1!", "hashed-pw")).willReturn(true);

        UserDto result = userService.login(request);

        assertThat(result.email()).isEqualTo("Woody@Monew.com");
        assertThat(result.nickname()).isEqualTo("우디");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword() {
        UserLoginRequest request = new UserLoginRequest("woody@monew.com", "wrong-pw");
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        given(userRepository.findByEmail("woody@monew.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-pw", "hashed-pw")).willReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_notFound() {
        UserLoginRequest request = new UserLoginRequest("none@monew.com", "monew1!");
        given(userRepository.findByEmail("none@monew.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("닉네임 수정 성공 - 본인")
    void updateNickname_success() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

        UserDto result = userService.updateNickname(userId, userId, new UserUpdateRequest("뉴우디"));

        assertThat(result.nickname()).isEqualTo("뉴우디");
    }

    @Test
    @DisplayName("닉네임 수정 실패 - 본인이 아님")
    void updateNickname_notSelf() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() ->
                userService.updateNickname(targetId, requesterId, new UserUpdateRequest("뉴우디")))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_ACCESS_DENIED);

        then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("논리 삭제 성공 - 본인")
    void delete_success() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

        userService.delete(userId, userId);

        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("논리 삭제 실패 - 본인이 아님")
    void delete_notSelf() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.delete(targetId, requesterId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("물리 삭제 성공 - 본인")
    void hardDelete_success() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        userService.hardDelete(userId, userId);

        then(userRepository).should().delete(user);
    }

    @Test
    @DisplayName("물리 삭제 실패 - 본인이 아님")
    void hardDelete_notSelf() {
        UUID targetId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.hardDelete(targetId, requesterId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_ACCESS_DENIED);

        then(userRepository).should(never()).delete(any(User.class));
    }
}
