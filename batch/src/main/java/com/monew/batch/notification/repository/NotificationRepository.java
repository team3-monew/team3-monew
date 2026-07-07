package com.monew.batch.notification.repository;

import com.monew.batch.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        select n.id
        from Notification n
        where n.confirmed = true
          and n.confirmedAt <= :threshold
        order by n.confirmedAt asc, n.id asc
    """)
    List<UUID> findConfirmedIdsBefore(
            @Param("threshold") LocalDateTime threshold,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Notification n
        where n.id in :ids
    """)
    int deleteAllByIdInBulk(@Param("ids") List<UUID> ids);
}