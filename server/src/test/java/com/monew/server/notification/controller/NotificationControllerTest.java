package com.monew.server.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.notification.dto.NotificationResponse;
import com.monew.server.notification.service.NotificationService;
import com.monew.server.support.ControllerTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest extends ControllerTestSupport {

    @MockitoBean
    NotificationService notificationService;

    @Test
    @DisplayName("미확인 알림 목록 조회 성공 - 요청 파라미터와 사용자 ID를 서비스로 전달한다")
    void findUnreadNotifications_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        NotificationResponse notification = new NotificationResponse(
                notificationId,
                createdAt,
                null,
                false,
                userId,
                "알림 내용",
                "comment",
                resourceId
        );
        CursorPageResponse<NotificationResponse> response = new CursorPageResponse<>(
                List.of(notification),
                notificationId.toString(),
                createdAt,
                1,
                1,
                false
        );

        given(notificationService.findUnreadNotifications(eq(userId), isNull(), isNull(), eq(10)))
                .willReturn(response);

        // when
        // then
        mockMvc.perform(get("/api/notifications")
                        .header("MoNew-Request-User-ID", userId)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].content").value("알림 내용"))
                .andExpect(jsonPath("$.content[0].resourceType").value("comment"))
                .andExpect(jsonPath("$.content[0].resourceId").value(resourceId.toString()))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("미확인 알림 목록 조회 성공 - OffsetDateTime 형식 after를 LocalDateTime으로 변환한다")
    void findUnreadNotifications_success_parseOffsetDateTimeAfter() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String cursor = UUID.randomUUID().toString();
        LocalDateTime expectedAfter = LocalDateTime.of(2026, 7, 1, 10, 0);
        CursorPageResponse<NotificationResponse> response = new CursorPageResponse<>(List.of(), null, null, 0, 0, false);

        given(notificationService.findUnreadNotifications(eq(userId), eq(cursor), eq(expectedAfter), eq(5)))
                .willReturn(response);

        // when
        // then
        mockMvc.perform(get("/api/notifications")
                        .header("MoNew-Request-User-ID", userId)
                        .param("cursor", cursor)
                        .param("after", "2026-07-01T10:00:00+09:00")
                        .param("limit", "5"))
                .andExpect(status().isOk());

        then(notificationService).should().findUnreadNotifications(userId, cursor, expectedAfter, 5);
    }

    @Test
    @DisplayName("미확인 알림 목록 조회 성공 - LocalDateTime 형식 after를 그대로 변환한다")
    void findUnreadNotifications_success_parseLocalDateTimeAfter() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String cursor = UUID.randomUUID().toString();
        LocalDateTime expectedAfter = LocalDateTime.of(2026, 7, 1, 10, 0);
        CursorPageResponse<NotificationResponse> response = new CursorPageResponse<>(List.of(), null, null, 0, 0, false);

        given(notificationService.findUnreadNotifications(eq(userId), eq(cursor), eq(expectedAfter), eq(5)))
                .willReturn(response);

        // when
        // then
        mockMvc.perform(get("/api/notifications")
                        .header("MoNew-Request-User-ID", userId)
                        .param("cursor", cursor)
                        .param("after", "2026-07-01T10:00:00")
                        .param("limit", "5"))
                .andExpect(status().isOk());

        then(notificationService).should().findUnreadNotifications(userId, cursor, expectedAfter, 5);
    }

    @Test
    @DisplayName("미확인 알림 목록 조회 실패 - after 형식이 올바르지 않으면 400을 반환한다")
    void findUnreadNotifications_fail_invalidAfter() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String cursor = UUID.randomUUID().toString();

        // when
        // then
        mockMvc.perform(get("/api/notifications")
                        .header("MoNew-Request-User-ID", userId)
                        .param("cursor", cursor)
                        .param("after", "invalid-after")
                        .param("limit", "5"))
                .andExpect(status().isBadRequest());

        then(notificationService).should(never()).findUnreadNotifications(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("알림 단건 확인 성공 - 서비스 호출 후 200 OK를 반환한다")
    void confirm_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isOk());

        then(notificationService).should().confirm(userId, notificationId);
    }

    @Test
    @DisplayName("알림 전체 확인 성공 - 서비스 호출 후 200 OK를 반환한다")
    void confirmAll_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(patch("/api/notifications")
                        .header("MoNew-Request-User-ID", userId))
                .andExpect(status().isOk());

        then(notificationService).should().confirmAll(userId);
    }

    @Test
    @DisplayName("알림 API 인증 실패 - 인증 헤더가 없으면 인터셉터에서 401을 반환한다")
    void notificationApi_fail_missingAuthHeader() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/api/notifications")
                        .param("limit", "10"))
                .andExpect(status().isUnauthorized());
    }
}
