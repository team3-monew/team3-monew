package com.monew.server.comment.service;

import com.monew.server.article.entity.Article;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentResponse;
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
import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
  public Comment createComment(CommentCreateRequest request, UUID userId) {
    Article article = articleRepository.findByIdAndDeletedAtIsNull(request.articleId())
        .orElseThrow(() -> new BaseException(ArticleErrorCode.ARTICLE_NOT_FOUND));
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    Comment comment = Comment.builder().article(article)
        .user(user).content(request.content()).build();
    return commentRepository.save(comment);

  }


  //댓글 수정(본인만 가능)
  @Transactional
  public Comment updateComment(UUID commentId, UUID userId, CommentUpdateRequest request) {
    Comment comment = getComment(commentId);
    if (!Objects.equals(comment.getUser().getId(), userId))
      throw new BaseException(CommonErrorCode.FORBIDDEN);

    comment.updateContent(request.content());
    return comment;
  }


  //댓글 삭제(논리 삭제)
  @Transactional
  public void deleteComment(UUID commentId, UUID userId) {
    Comment comment = getComment(commentId);
    if (!Objects.equals(comment.getUser().getId(), userId))
      throw new BaseException(CommonErrorCode.FORBIDDEN);

    comment.delete();
  }


  //댓글 좋아요, 좋아요 취소
  // 좋아요 추가
  @Transactional
  public void addLike(UUID commentId, UUID userId) {
    if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
      throw new BaseException(CommonErrorCode.INVALID_REQUEST); // 이미 좋아요 상태
    }
    Comment comment = getComment(commentId);
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    commentLikeRepository.save(CommentLike.builder().comment(comment).user(user).build());
    comment.increaseLikeCount();
  }

  // 좋아요 취소
  @Transactional
  public void removeLike(UUID commentId, UUID userId) {
    CommentLike like = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
        .orElseThrow(() -> new BaseException(CommonErrorCode.INVALID_REQUEST)); // 좋아요 하지 않은 상태
    like.getComment().decreaseLikeCount();
    commentLikeRepository.delete(like);
  }



  //Controller 호출용
  public Comment getComment(UUID commentId) {
    return commentRepository.findById(commentId)
        .orElseThrow(() -> new BaseException(CommentErrorCode.COMMENT_NOT_FOUND));
  }

  public CommentResponse getCommentResponse(UUID commentId, UUID userId) {
    return CommentResponse.of(getComment(commentId),
        commentLikeRepository.existsByCommentIdAndUserId(commentId, userId));
  }

  public CommentResponse createCommentResponse(CommentCreateRequest request, UUID userId) {
    return CommentResponse.of(createComment(request, userId), false);
  }

  public CommentResponse updateCommentResponse(UUID commentId, UUID userId,
      CommentUpdateRequest request) {
    return CommentResponse.of(updateComment(commentId, userId, request),
        commentLikeRepository.existsByCommentIdAndUserId(commentId, userId));
  }

  public CommentResponse addLikeAndGetResponse(UUID commentId, UUID userId) {
    addLike(commentId, userId);
    return CommentResponse.of(getComment(commentId), true);
  }




// 댓글 목록 조회(데이터 처리 계층 - 데이터 준비)
  public CommentSliceResult getCommentsByArticleCursor(
      UUID articleId, String sortBy, String direction, LocalDateTime lastCreatedAt,
      Long lastLikeCount, UUID lastId, int size) {

    Pageable pageable = PageRequest.of(0, size + 1);

    List<Comment> comments;
    if ("LIKE".equalsIgnoreCase(sortBy) || "likeCount".equalsIgnoreCase(sortBy)) {
      comments = commentRepository.findCommentsByArticleLikeCursor(articleId, lastLikeCount, lastId, direction, pageable);
    } else {
      comments = commentRepository.findCommentsByArticleValueCursor(articleId, lastCreatedAt, lastId, direction, pageable);
    }

    boolean hasNext = comments.size() > size;
    if (hasNext) {
      comments = comments.subList(0, size);
    }

    String nextCursor = null;
    LocalDateTime nextAfter = null;

    if (!comments.isEmpty()) {
      Comment lastComment = comments.get(comments.size() - 1);

      // 정렬 조건이 LIKE면 좋아요 수, 아니면 생성일을 문자열로 바인딩
      nextCursor = ("LIKE".equalsIgnoreCase(sortBy) || "likeCount".equalsIgnoreCase(sortBy))
          ? String.valueOf(lastComment.getLikeCount())
          : lastComment.getCreatedAt().toString();

      nextAfter = lastComment.getCreatedAt();
    }

    return new CommentSliceResult(comments, nextCursor, nextAfter, hasNext, size);
  }


  //댓글 목록 조회(비즈니스 게층 - 응답 조립)
  public CursorPageResponse<CommentResponse> getComments(
      UUID articleId, String orderBy, String direction, String cursor, LocalDateTime after,
      int limit, UUID userId
  ) {
    UUID lastId = parseLastIdFromCursor(cursor);
    Long lastLikeCount =
        "likeCount".equalsIgnoreCase(orderBy) && cursor != null ? parseLikeCountFromCursor(cursor)
            : null;
    LocalDateTime lastCreatedAt =
        "createdAt".equalsIgnoreCase(orderBy) && cursor != null ? parseCreatedAtFromCursor(cursor)
            : after;

    long totalCount = commentRepository.countByArticleIdAndDeletedAtIsNull(articleId);

    CommentSliceResult result = getCommentsByArticleCursor(articleId, orderBy, direction,
        lastCreatedAt,
        lastLikeCount, lastId, limit);

    List<CommentResponse> commentResponses = result.content().stream()
        .map(comment -> {
          boolean likedByMe = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(),
              userId);
          return CommentResponse.of(comment, likedByMe);
        })
        .toList();

    return new CursorPageResponse<>(
        commentResponses,
        result.nextCursor(),
        result.nextAfter(),
        result.size(),
        (int) totalCount,
        result.hasNext()
    );

  }

  // 커서 파싱 메서드들
  private UUID parseLastIdFromCursor(String cursor) {
    if (cursor == null || !cursor.contains(":"))
      return null;
    try {
      String idPart = cursor.split(":")[1];
      return UUID.fromString(idPart);
    } catch (Exception e) {
      return null;
    }
  }

  private Long parseLikeCountFromCursor(String cursor) {
    if (cursor == null || !cursor.contains(":"))
      return null;
    try {
      return Long.parseLong(cursor.split(":")[0]);
    } catch (Exception e) {
      return null;
    }
  }

  private LocalDateTime parseCreatedAtFromCursor(String cursor) {
    if (cursor == null || !cursor.contains(":"))
      return null;
    try {
      return LocalDateTime.parse(cursor.split(":")[0]);
    } catch (Exception e) {
      return null;
    }
  }

}