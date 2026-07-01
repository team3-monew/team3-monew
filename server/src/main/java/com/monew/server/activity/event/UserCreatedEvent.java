package com.monew.server.activity.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(
  UUID userId,
  String email,
  String nickname,
  LocalDateTime createdAt
){
}
