package com.monew.batch.notification.repository;

import com.monew.batch.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Notification n
        where n.confirmed = true
          and n.confirmedAt <= :threshold
    """)
    int deleteConfirmedBefore(@Param("threshold") LocalDateTime threshold);
}