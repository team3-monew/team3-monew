package com.monew.server.comment.entity;

import com.monew.server.common.entity.BaseCreatedEntity;
import com.monew.server.user.entity.User;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comment_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
public class CommentLike extends BaseCreatedEntity {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comment_id")
  @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
  private Comment comment;


  @PrePersist
  void prePersist() {
      if (id == null) {
          id = UUID.randomUUID();
      }

  }

  @Builder
  public CommentLike(User user, Comment comment) {
      this.user = user;
      this.comment = comment;
  }
}
