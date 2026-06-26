package com.monew.server.comment.service;

import com.monew.server.article.entity.Article;
import com.monew.server.article.repository.ArticleRepository; // 기사 조회용 가정
import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.entity.CommentLike;
import com.monew.server.comment.repository.CommentLikeRepository;
import com.monew.server.comment.repository.CommentRepository;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository; // 유저 조회용 가정
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;


  //댓글 등록
  @Transactional
  public UUID createComment(UUID articleId, UUID userId, String content) {

    Article article = articleRepository.findById(articleId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기사입니다."));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));


    Comment comment = Comment.builder()
        .article(article)
        .user(user)
        .content(content)
        .build();

    commentRepository.save(comment);
    return comment.getId();
  }


  //댓글 수정(본인만 가능)
  @Transactional
  public void updateComment(UUID commentId, UUID userId, String newContent) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalStateException("본인이 작성한 댓글만 수정할 수 있습니다.");
    }

    comment.updateContent(newContent);
  }



  //댓글 삭제(논리 삭제)
  @Transactional
  public void deleteComment(UUID commentId, UUID userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalStateException("본인이 작성한 댓글만 삭제할 수 있습니다.");
    }

    comment.delete();
  }


  //댓글 좋아요, 좋아요 취소
  @Transactional
  public void toggleCommentLike(UUID commentId, UUID userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

    if (existingLike.isPresent()) {
      // 이미 존재하면 -> 좋아요 취소
      commentLikeRepository.delete(existingLike.get());
      comment.decreaseLikeCount(); // 댓글 엔티티 카운트 -1
    } else {
      // 존재하지 않으면 -> 좋아요 누르기
      CommentLike commentLike = CommentLike.builder()
          .comment(comment)
          .user(user)
          .build();
      commentLikeRepository.save(commentLike);
      comment.increaseLikeCount(); // 댓글 엔티티 카운트 +1

      // 💡 [알림 관리 요구사항 참고]: 여기에 나중에 알림 생성 로직 추가 예정!
      // 예: notificationService.createNotification(comment.getUser(), "[사용자]님이 나의 댓글을 좋아합니다.", comment);
    }
  }



}