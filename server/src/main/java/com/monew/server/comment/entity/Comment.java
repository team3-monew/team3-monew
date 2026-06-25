package com.monew.server.comment.entity;

import com.monew.server.article.entity.Article;
import com.monew.server.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

  @Id
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id")
  private Article article;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;
  @Column(nullable = false, length = 500)
  private String content;
  @Column(name = "like_count", nullable = false)
  private long likeCount;
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;
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
