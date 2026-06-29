package com.monew.server.common.exception.activity;

import com.monew.server.common.exception.BaseException;

public class UserActivityNotFoundException extends BaseException {

  public UserActivityNotFoundException(String userId) {
    super(ActivityErrorCode.USER_ACTIVITY_NOT_FOUND);
    addDetail("userId", userId);
  }
}