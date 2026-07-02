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
import com.monew.server.activity.event.UserCreatedEvent;
import com.monew.server.activity.event.UserNicknameUpdatedEvent;
import com.monew.server.activity.event.UserDeletedEvent;


/**
 * {@link UserService} 단위 테스트 (Mockito).
 *
 * <p>Repository/PasswordEncoder/EventPublisher 를 mock 하여 서비스 로직만 검증
 * 커버 범위: 회원가입(중복·트림·이벤트), 로그인(성공·실패·탈퇴 사용자),
 * 닉네임 수정/논리삭제/물리삭제(본인 확인·존재 확인·이벤트).
 *
 * <p>인가(본인 확인)는 서비스 책임이므로 여기서 검증하고,
 * 인증(헤더 검증)은 AuthInterceptorTest 에서 별도로 검증
 */
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

    // ───────────────────────── 보강: 이벤트 발행 / 트림 / 미존재 / 탈퇴 ─────────────────────────

    @Test
    @DisplayName("회원가입 - 이메일 앞뒤 공백 제거(trim) 후 저장 및 UserCreatedEvent 발행")
    void register_trimsEmailAndPublishesEvent() {
        UserRegisterRequest request = new UserRegisterRequest("  woody@monew.com  ", "우디", "monew1!");
        given(userRepository.existsByEmail("woody@monew.com")).willReturn(false); // trim 된 값으로 중복 확인
        given(passwordEncoder.encode("monew1!")).willReturn("hashed-pw");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.register(request);

        assertThat(result.email()).isEqualTo("woody@monew.com");        // trim 적용됨
        then(passwordEncoder).should().encode("monew1!");                // 비밀번호는 해싱해서 저장
        then(eventPublisher).should().publishEvent(any(UserCreatedEvent.class)); // 활동내역용 이벤트 발행
    }

    @Test
    @DisplayName("로그인 실패 - 논리 삭제된(탈퇴) 사용자는 비밀번호 비교 전에 차단")
    void login_deletedUser() {
        UserLoginRequest request = new UserLoginRequest("woody@monew.com", "monew1!");
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        user.delete(); // 탈퇴 상태
        given(userRepository.findByEmail("woody@monew.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.LOGIN_FAILED);

        then(passwordEncoder).should(never()).matches(any(), any()); // 탈퇴면 비번 비교까지 가지 않음
    }

    @Test
    @DisplayName("닉네임 수정 - UserNicknameUpdatedEvent 발행")
    void updateNickname_publishesEvent() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

        userService.updateNickname(userId, userId, new UserUpdateRequest("뉴우디"));

        then(eventPublisher).should().publishEvent(any(UserNicknameUpdatedEvent.class));
    }

    @Test
    @DisplayName("닉네임 수정 실패 - 대상 사용자 없음(USER_NOT_FOUND)")
    void updateNickname_notFound() {
        UUID userId = UUID.randomUUID();
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.updateNickname(userId, userId, new UserUpdateRequest("뉴우디")))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("논리 삭제 - UserDeletedEvent 발행")
    void delete_publishesEvent() {
        UUID userId = UUID.randomUUID();
        User user = new User("woody@monew.com", "우디", "hashed-pw");
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));

        userService.delete(userId, userId);

        then(eventPublisher).should().publishEvent(any(UserDeletedEvent.class));
    }

    @Test
    @DisplayName("논리 삭제 실패 - 대상 사용자 없음(USER_NOT_FOUND)")
    void delete_notFound() {
        UUID userId = UUID.randomUUID();
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(userId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("물리 삭제 실패 - 대상 사용자 없음(USER_NOT_FOUND)")
    void hardDelete_notFound() {
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.hardDelete(userId, userId))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode").isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
}
