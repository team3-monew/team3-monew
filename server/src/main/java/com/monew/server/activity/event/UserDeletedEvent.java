package com.monew.server.activity.event;

import java.util.UUID;

public record UserDeletedEvent(
    UUID userId
) {
}
