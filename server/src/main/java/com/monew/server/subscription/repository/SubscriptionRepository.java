package com.monew.server.subscription.repository;

import com.monew.server.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUser_IdAndInterest_Id(UUID userId, UUID interestId);

    boolean existsByUser_IdAndInterest_Id(UUID userId, UUID interestId);

    @Query("""
        select s.interest.id
        from Subscription s
        where s.user.id = :userId
          and s.interest.id in :interestIds
    """)
    List<UUID> findSubscribedInterestIds(
            @Param("userId") UUID userId,
            @Param("interestIds") Collection<UUID> interestIds
    );

    // 특정 관심사를 구독한 사용자 ID 목록 조회(알림 기능에 필요할 것 같아서 넣었습니다)
    @Query("""
        select s.user.id
        from Subscription s
        where s.interest.id = :interestId
    """)
    List<UUID> findSubscriberUserIdsByInterestId(@Param("interestId") UUID interestId);
}
