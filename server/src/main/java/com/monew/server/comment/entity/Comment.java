package com.monew.server.comment.entity;

import com.monew.server.article.entity.Article;
import com.monew.server.common.entity.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

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

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UUID.randomUUID();
    }

    this.likeCount = 0;
  }

  @Builder
  public Comment(Article article, User user, String content) {
    this.article = article;
    this.user = user;
    this.content = content;
  }

  //댓글 수정
  public void updateContent(String newContent) {
    this.content = newContent;
  }

  //논리 삭제
  public void delete() {
    // comments 테이블에는 is_deleted 컬럼 없이 deleted_at만 있으므로
    // 삭제 여부는 deletedAt에 값이 있는지로 판단한다.
    this.deletedAt = LocalDateTime.now();
  }

  //좋아요 증가
  public void increaseLikeCount() {
    this.likeCount += 1;
  }

  //좋아요 취소(감소)
  public void decreaseLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount -= 1;
    }
  }
}