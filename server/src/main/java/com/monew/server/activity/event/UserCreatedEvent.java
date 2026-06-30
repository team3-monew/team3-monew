package com.monew.server.activity.event;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
  UUID userId,
  String email,
  String nickname,
  Instant createdAt
){
}