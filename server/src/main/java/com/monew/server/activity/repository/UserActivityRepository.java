package com.monew.server.activity.repository;

import com.monew.server.activity.document.UserActivity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivity, String> {
}
