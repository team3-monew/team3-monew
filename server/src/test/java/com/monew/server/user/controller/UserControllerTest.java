package com.monew.server.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.server.support.ControllerTestSupport;
import com.monew.server.user.dto.UserDto;
import com.monew.server.user.dto.UserLoginRequest;
import com.monew.server.user.dto.UserRegisterRequest;
import com.monew.server.user.dto.UserUpdateRequest;
import com.monew.server.user.service.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserController.class)
class UserControllerTest extends ControllerTestSupport {

    @MockitoBean
    UserService userService;

    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    void register_success() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest("woody@monew.com", "우디", "monew1!");
        UserDto response = new UserDto(UUID.randomUUID(), "woody@monew.com", "우디", LocalDateTime.now());
        given(userService.register(any())).willReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("woody@monew.com"))
                .andExpect(jsonPath("$.nickname").value("우디"));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 오류 400")
    void register_invalidEmail() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest("not-an-email", "우디", "monew1!");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공 - 200 OK")
    void login_success() throws Exception {
        UserLoginRequest request = new UserLoginRequest("woody@monew.com", "monew1!");
        UserDto response = new UserDto(UUID.randomUUID(), "woody@monew.com", "우디", LocalDateTime.now());
        given(userService.login(any())).willReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("woody@monew.com"));
    }

    @Test
    @DisplayName("닉네임 수정 성공 - 200 OK")
    void update_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("뉴우디");
        UserDto response = new UserDto(userId, "woody@monew.com", "뉴우디", LocalDateTime.now());
        given(userService.updateNickname(any(), any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .header("MoNew-Request-User-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("뉴우디"));
    }

    @Test
    @DisplayName("닉네임 수정 실패 - 인증 헤더 누락 401 (인터셉터 차단)")
    void update_missingHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("뉴우디");

        mockMvc.perform(patch("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("논리 삭제 성공 - 204 No Content")
    void delete_success() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("물리 삭제 성공 - 204 No Content")
    void hardDelete_success() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}/hard", userId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isNoContent());
    }
}
