package com.monew.server.notification.repository;

import com.monew.server.notification.entity.Notification;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndUserId(UUID notificationId, UUID userId);

}