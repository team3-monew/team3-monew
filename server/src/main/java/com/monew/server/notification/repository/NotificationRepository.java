package com.monew.server.notification.repository;

import com.monew.server.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndUserId(UUID notificationId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Notification n
    set n.confirmed = true,
        n.confirmedAt = :confirmedAt
    where n.user.id = :userId
      and n.confirmed = false
""")
    int confirmAllByUserId(UUID userId, LocalDateTime confirmedAt);

}