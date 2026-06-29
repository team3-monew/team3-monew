package com.monew.server.activity.controller;

import com.monew.server.activity.dto.UserActivityResponse;
import com.monew.server.activity.service.UserActivityService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-activities")
public class UserActivityController {

  private final UserActivityService userActivityService;

  @GetMapping("/{userId}")
  public ResponseEntity<UserActivityResponse> getUserActivity(
      @PathVariable UUID userId
  ) {
    UserActivityResponse response = userActivityService.getUserActivity(userId.toString());

    return ResponseEntity.ok(response);
  }


}
