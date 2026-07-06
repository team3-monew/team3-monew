package com.monew.server.notification.controller;

import com.monew.server.common.exception.notification.NotificationErrorCode;
import com.monew.server.common.exception.notification.NotificationException;
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.common.security.LoginUser;
import com.monew.server.notification.dto.NotificationResponse;
import com.monew.server.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<CursorPageResponse<NotificationResponse>> findUnreadNotifications(
            @LoginUser UUID userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String after,
            @RequestParam int limit
    ) {
        CursorPageResponse<NotificationResponse> response =
                notificationService.findUnreadNotifications(
                        userId,
                        cursor,
                        parseAfter(after),
                        limit
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}")
    public ResponseEntity<Void> confirm(
            @LoginUser UUID userId,
            @PathVariable UUID notificationId
    ) {
        notificationService.confirm(userId, notificationId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping
    public ResponseEntity<Void> confirmAll(
            @LoginUser UUID userId
    ) {
        notificationService.confirmAll(userId);

        return ResponseEntity.ok().build();
    }

    private LocalDateTime parseAfter(String after) {
        if (after == null || after.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(after)
                    .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(after);
            } catch (DateTimeParseException e) {
                NotificationException exception =
                        new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_CURSOR, e);
                exception.addDetail("after", after);
                throw exception;
            }
        }
    }
}