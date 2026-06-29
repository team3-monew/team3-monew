package com.monew.server.activity.service;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.dto.UserActivityResponse;
import com.monew.server.activity.repository.UserActivityRepository;
import com.monew.server.common.exception.activity.UserActivityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityService {

  private final UserActivityRepository userActivityRepository;

  public UserActivityResponse getUserActivity(String userId){
    UserActivity userActivity = userActivityRepository.findById(userId)
        .orElseThrow(() -> new UserActivityNotFoundException(userId));
    return UserActivityResponse.from(userActivity);
  }
}
