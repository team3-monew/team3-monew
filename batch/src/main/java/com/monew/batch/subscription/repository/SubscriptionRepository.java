package com.monew.batch.subscription.repository;

import java.util.List;
import java.util.UUID;

import com.monew.batch.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
            select s.user.id
            from Subscription s
            where s.interest.id = :interestId
            """)
    List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);
}