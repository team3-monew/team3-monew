package com.monew.server.comment.service;

import com.monew.server.article.entity.Article;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentSliceResult;
import com.monew.server.comment.dto.CommentUpdateRequest;
import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.entity.CommentLike;
import com.monew.server.comment.repository.CommentLikeRepository;
import com.monew.server.comment.repository.CommentRepository;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import com.monew.server.common.exception.article.ArticleErrorCode;
import com.monew.server.common.exception.comment.CommentErrorCode;
import com.monew.server.common.exception.user.UserErrorCode;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
  public UUID createComment(CommentCreateRequest request, UUID userId) {

    Article article = articleRepository.findByIdAndDeletedAtIsNull(request.articleId())
        .orElseThrow(() -> new BaseException(ArticleErrorCode.ARTICLE_NOT_FOUND));
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));


    Comment comment = Comment.builder()
        .article(article)
        .user(user)
        .content(request.content())
        .build();

    commentRepository.save(comment);
    return comment.getId();
  }


  //댓글 수정(본인만 가능)
  @Transactional
  public Comment updateComment(UUID commentId, UUID userId, CommentUpdateRequest request) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new BaseException(CommentErrorCode.COMMENT_NOT_FOUND));

    if (!Objects.equals(comment.getUser().getId(), userId)) {
      throw new BaseException(CommonErrorCode.FORBIDDEN);
    }
    comment.updateContent(request.content());

    return comment;
  }



  //댓글 삭제(논리 삭제)
  @Transactional
  public void deleteComment(UUID commentId, UUID userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new BaseException(CommentErrorCode.COMMENT_NOT_FOUND));

    if (!Objects.equals(comment.getUser().getId(), userId)) {
      throw new BaseException(CommonErrorCode.FORBIDDEN);
    }
    comment.delete();
  }


  //댓글 좋아요, 좋아요 취소
  @Transactional
  public void toggleCommentLike(UUID commentId, UUID userId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new BaseException(CommentErrorCode.COMMENT_NOT_FOUND));
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

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

    }
  }


  //뉴스 기사 별 댓글 목록 조회
  public CommentSliceResult getCommentsByArticleCursor
      (UUID articleId, String sortBy, LocalDateTime lastCreatedAt, Long lastLikeCount, UUID lastId,int size) {

    Pageable pageable = PageRequest.of(0, size + 1);
    List<Comment> comments;

    if ("LIKE".equalsIgnoreCase(sortBy) || "likeCount".equalsIgnoreCase(sortBy)) {
      comments = commentRepository.findCommentsByArticleLikeCursor(articleId, lastLikeCount, lastId, pageable);
    } else {
      comments = commentRepository.findCommentsByArticleValueCursor(articleId, lastCreatedAt, lastId, pageable);
    }

    //다음 페이지가 있는지 확인
    boolean hasNext = comments.size() > size;
    if (hasNext) {
      comments = comments.subList(0, size);
    }

    //공통 스펙에 넣을 커서 변수 초기화
    String nextCursor = null;
    LocalDateTime nextAfter = null;

    //데이터가 존재한다면 마지막 조각을 기준으로 커서 계산
    if (!comments.isEmpty()) {
      Comment lastComment = comments.get(comments.size() - 1);

      //정렬 조건이 LIKE면 좋아요 수, 아니면 생성일을 문자열로 바인딩
      nextCursor = ("LIKE".equalsIgnoreCase(sortBy) || "likeCount".equalsIgnoreCase(sortBy))
          ? String.valueOf(lastComment.getLikeCount())
          : lastComment.getCreatedAt().toString();

      nextAfter = lastComment.getCreatedAt();
    }

    return new CommentSliceResult(comments, nextCursor, nextAfter, hasNext, size);
  }

}