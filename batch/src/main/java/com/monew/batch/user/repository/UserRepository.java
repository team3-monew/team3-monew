package com.monew.batch.user.repository;

import com.monew.batch.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 물리삭제 대상(논리삭제 후 기준 시각 이전) 사용자 ID 목록.
    // Mongo 활동내역도 함께 지우려면 삭제 '전에' ID를 확보해야 함.
    @Query("select u.id from User u where u.deletedAt is not null and u.deletedAt < :threshold")
    List<UUID> findIdsByDeletedAtBefore(@Param("threshold") LocalDateTime threshold);

    // 지정 ID 사용자 물리 삭제 (연관 데이터는 DB FK ON DELETE CASCADE로 함께 삭제)
    @Modifying
    @Query("delete from User u where u.id in :ids")
    int deleteAllByIdIn(@Param("ids") List<UUID> ids);
}
