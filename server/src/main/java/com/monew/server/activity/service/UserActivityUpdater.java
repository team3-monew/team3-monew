package com.monew.server.activity.service;

import com.monew.server.activity.document.UserActivity;
import com.monew.server.activity.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserActivityUpdater {

  private final UserActivityRepository userActivityRepository;
  private final MongoTemplate mongoTemplate;

  // 회원가입 시 사용자 활동 문서를 최초 생성합니다
  public void create(UserActivity userActivity) {
    userActivityRepository.save(userActivity);
  }

  // 사용자가 닉네임을 수정하면 MongoDB 조회 모델도 변경합니다.
  public void updateNickname(String userId, String nickname) {
    Query query = findUser(userId);
    Update update = new Update().set("nickname", nickname);

    mongoTemplate.updateFirst(query, update, UserActivity.class);
  }

  private Query findUser(String userId) {
    return Query.query(Criteria.where("_id").is(userId));
  }
}
