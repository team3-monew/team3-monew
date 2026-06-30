package com.monew.server.activity.event;

import java.util.UUID;

public record UserNicknameUpdatedEvent(
    UUID userId,
    String nickname
) {
}
