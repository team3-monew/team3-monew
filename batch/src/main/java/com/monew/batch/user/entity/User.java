package com.monew.batch.user.entity;

import com.monew.batch.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 320)
  private String email;

  @Column(nullable = false, length = 20)
  private String nickname;

  @Column(nullable = false)            // BCrypt 해시(60자) -> 기본 255
  private String password;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

}
