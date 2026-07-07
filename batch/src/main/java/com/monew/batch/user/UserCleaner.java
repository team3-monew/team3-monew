package com.monew.batch.user;

import com.monew.batch.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 물리삭제 로직.
 * 요구사항: 물리 삭제 시 관련 정보도 모두 삭제 → PostgreSQL(FK CASCADE) + MongoDB 활동내역까지.
 * 배치는 in-process 이벤트로 server의 Mongo 리스너를 부를 수 없어 Mongo를 직접 삭제한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCleaner {

    static final String USER_ACTIVITIES_COLLECTION = "user_activities";

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional
    public void cleanup(LocalDateTime threshold) {
        // 물리삭제 대상 ID 확보 (삭제 '전에' — Mongo 활동내역도 같은 ID로 지워야 하므로)
        List<UUID> targetIds = userRepository.findIdsByDeletedAtBefore(threshold);
        if (targetIds.isEmpty()) {
            log.info("[userCleanup] 물리 삭제 대상 없음, 기준={} 이전", threshold);
            return;
        }

        // 1) Mongo 활동내역 삭제 먼저 (실패 시 유저는 PG에 남아 다음 회차 재시도 — 유령 활동내역 방지)
        //    user_activities._id = userId(String)
        List<String> idStrings = targetIds.stream().map(UUID::toString).toList();
        long mongoDeleted = mongoTemplate.remove(
            Query.query(Criteria.where("_id").in(idStrings)), USER_ACTIVITIES_COLLECTION
        ).getDeletedCount();

        // 2) PostgreSQL 물리 삭제 (연관 데이터는 FK ON DELETE CASCADE로 함께 삭제)
        int pgDeleted = userRepository.deleteAllByIdIn(targetIds);

        log.info("[userCleanup] 물리삭제 PG={}, Mongo활동내역={}, 기준={} 이전", pgDeleted, mongoDeleted, threshold);
    }
}
