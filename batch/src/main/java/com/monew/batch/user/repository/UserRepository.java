package com.monew.batch.user.repository;

import com.monew.batch.user.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 논리삭제 후 기준 시각 이전인 사용자 물리 삭제
    // 연관 데이터는 DB FK(ON DELETE CASCADE)로 함께 삭제
    @Modifying
    @Query("delete from User u where u.deletedAt is not null and u.deletedAt < :threshold")
    int deleteAllByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);
}
