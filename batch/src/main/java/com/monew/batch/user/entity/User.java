package com.monew.batch.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

  @Id
  private UUID id;
  @Column(nullable = false, unique = true)
  private String email;
  @Column(nullable = false, length = 20)
  private String nickname;
  @Column(nullable = false)
  private String password;
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  void prePersist() {
      if (id == null) {
          id = UUID.randomUUID();
      }
      if (createdAt == null) {
          createdAt = LocalDateTime.now();
      }
  }
}
