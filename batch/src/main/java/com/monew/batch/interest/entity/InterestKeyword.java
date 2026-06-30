package com.monew.batch.interest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "interest_keywords", uniqueConstraints = @UniqueConstraint(columnNames = {
    "interest_id", "keyword"}))
public class InterestKeyword {

  @Id
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "interest_id")
  private Interest interest;
  @Column(nullable = false, length = 50)
  private String keyword;
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

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
