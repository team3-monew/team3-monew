package com.monew.server.comment.service;

import com.monew.server.article.entity.Article;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentLikeResponse;
import com.monew.server.comment.dto.CommentResponse;
import com.monew.server.comment.dto.CommentSliceResult;
import com.monew.server.comment.dto.CommentSortBy;
import com.monew.server.comment.dto.CommentSortDirection;
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
import com.monew.server.notification.service.NotificationService; // 추가된 import
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

  //[변경]
  // limit 상한 상수 추가 — 커서 페이지네이션에 과도한 값이 들어오는 것 방지
  private static final int MAX_LIMIT = 50;

  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleRepository articleRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService; // 알림 서비스 주입

  //댓글 등록 // 간단히 그냥 카운트 처리 하는거라 제작 레포에서 호출하는거 추가한 구조입니다
  @Transactional
  public Comment createComment(CommentCreateRequest request, UUID userId) {
    Article article = articleRepository.findActiveByIdForUpdate(request.articleId())
            .orElseThrow(() -> new BaseException(ArticleErrorCode.ARTICLE_NOT_FOUND));
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    Comment comment = Comment.builder().article(article)
            .user(user).content(request.content()).build();

    Comment savedComment = commentRepository.save(comment);
    articleRepository.increaseCommentCount(article.getId());
    return savedComment;

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

  // [변경]
  // findActiveByIdForUpdate 사전 조회 제거
  // 이유: 요구사항 문서상 "관련된 정보가 유지되도록 논리 삭제를 기본 원칙으로 하세요"라고 명시되어 있어,
  // 기사가 논리 삭제된 뒤에도 댓글은 독립적으로 존재/조작 가능해야 함.
  // decreaseCommentCount 쿼리 자체가 이미 deletedAt IS NULL 조건을 갖고 있어
  // 기사가 삭제된 상태에서 호출돼도 예외 없이 안전하게 0 row 처리됨.
  @Transactional
  public void deleteComment(UUID commentId, UUID userId) {
    Comment comment = getComment(commentId);

    //이미 삭제된 경우 예외 처리
    if (comment.getDeletedAt() != null) {
      throw new BaseException(CommonErrorCode.INVALID_REQUEST);
    }

    if (!Objects.equals(comment.getUser().getId(), userId))
      throw new BaseException(CommonErrorCode.FORBIDDEN);

    comment.delete();
    articleRepository.decreaseCommentCount(comment.getArticle().getId());
  }


  //댓글 좋아요, 좋아요 취소
  // 좋아요 추가
  // [변경]
  // existsBy 체크 후 save → insertIgnore 기반 원자적 처리로 재작성 (TOCTOU 방지)
  @Transactional
  public CommentLike addLike(UUID commentId, UUID userId) {
    Comment comment = getComment(commentId);
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    UUID likeId = UUID.randomUUID();
    int inserted = commentLikeRepository.insertIgnore(likeId, commentId, userId);
    if (inserted == 0) {
      // 영향받은 row가 0건 = UNIQUE(comment_id, user_id) 충돌 = 이미 좋아요 상태
      throw new BaseException(CommonErrorCode.INVALID_REQUEST);
    }

    // [변경]
    // comment.increaseLikeCount() → 원자적 UPDATE 쿼리 호출
    commentRepository.increaseLikeCount(commentId);

    notificationService.createCommentLikeNotification(
        comment.getUser().getId(),
        userId,
        comment.getId(),
        user.getNickname()
    );

    // insertIgnore는 native insert라 영속성 컨텍스트에 엔티티가 없으므로 재조회
    return commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
        .orElseThrow(() -> new BaseException(CommentErrorCode.COMMENT_NOT_FOUND));
  }


  // 좋아요 취소
  // [변경]
  // like.getComment().decreaseLikeCount() → 원자적 UPDATE 쿼리 호출
  @Transactional
  public void removeLike(UUID commentId, UUID userId) {
    CommentLike like = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
        .orElseThrow(() -> new BaseException(CommonErrorCode.INVALID_REQUEST));
    commentLikeRepository.delete(like);
    commentRepository.decreaseLikeCount(commentId);
  }


  //Controller 호출용
  public Comment getComment(UUID commentId) {
    return commentRepository.findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> new BaseException(CommentErrorCode.COMMENT_NOT_FOUND));
  }

  public CommentResponse getCommentResponse(UUID commentId, UUID userId) {
    return CommentResponse.of(getComment(commentId),
        commentLikeRepository.existsByCommentIdAndUserId(commentId, userId));
  }

  @Transactional
  public CommentResponse createCommentResponse(CommentCreateRequest request, UUID userId) {
    return CommentResponse.of(createComment(request, userId), false);
  }

  @Transactional
  public CommentResponse updateCommentResponse(UUID commentId, UUID userId,
      CommentUpdateRequest request) {
    return CommentResponse.of(updateComment(commentId, userId, request),
        commentLikeRepository.existsByCommentIdAndUserId(commentId, userId));
  }

  // [변경]
  // increaseLikeCount가 clearAutomatically=true라 영속성 컨텍스트가 비워짐
  // → commentLike.getComment()는 stale한 likeCount를 들고 있을 수 있어 재조회 필요
  @Transactional
  public CommentLikeResponse addLikeAndGetResponse(UUID commentId, UUID userId) {
    CommentLike commentLike = addLike(commentId, userId);
    Comment refreshed = getComment(commentId);
    return CommentLikeResponse.of(commentLike, refreshed);
  }



  // 댓글 목록 조회(데이터 처리 계층 - 데이터 준비)
  public CommentSliceResult getCommentsByArticleCursor(
      UUID articleId, String sortBy, String direction, LocalDateTime lastCreatedAt,
      Long lastLikeCount, UUID lastId, int size) {

    Pageable pageable = PageRequest.of(0, size + 1);

    List<Comment> comments =
        ("LIKE".equalsIgnoreCase(sortBy) || "likeCount".equalsIgnoreCase(sortBy))
            ? commentRepository.findCommentsByArticleLikeCursor(articleId, lastLikeCount, lastId,
            direction, pageable)
            : commentRepository.findCommentsByArticleValueCursor(articleId, lastCreatedAt, lastId,
                direction, pageable);

    boolean hasNext = comments.size() > size;
    if (hasNext) {
      comments = comments.subList(0, size);
    }

    String nextCursor = null;
    LocalDateTime nextAfter = null;

    if (!comments.isEmpty()) {
      Comment lastComment = comments.get(comments.size() - 1);

      // 정렬 조건이 LIKE면 좋아요 수, 아니면 생성일을 문자열로 바인딩
      String sortValue = ("LIKE".equalsIgnoreCase(sortBy) || "likeCount".equalsIgnoreCase(sortBy))
          ? String.valueOf(lastComment.getLikeCount())
          : lastComment.getCreatedAt().toString();

      nextCursor = sortValue + ":" + lastComment.getId().toString();
      nextAfter = lastComment.getCreatedAt();
    }

    return new CommentSliceResult(comments, nextCursor, nextAfter, hasNext, size);
  }


  //댓글 목록 조회(비즈니스 계층 - 응답 조립)
  // [변경] orderBy/direction을 Enum으로 검증 + limit 상한 적용
  public CursorPageResponse<CommentResponse> getComments(
      UUID articleId, String orderBy, String direction, String cursor, LocalDateTime after,
      int limit, UUID userId
  ) {
    // [변경]
    // 잘못된 정렬 파라미터는 여기서 즉시 400으로 차단 (fail-fast)
    // CommentErrorCode를 건드릴 수 없어, 이미 쓰이고 있는 CommonErrorCode.INVALID_REQUEST로 통일
    CommentSortBy sortBy;
    CommentSortDirection sortDirection;
    try {
      sortBy = CommentSortBy.from(orderBy);
      sortDirection = CommentSortDirection.from(direction);
    } catch (IllegalArgumentException e) {
      throw new BaseException(CommonErrorCode.INVALID_REQUEST);
    }

    // [변경] 클라이언트가 큰 값을 보내도 서버가 상한을 강제
    int safeLimit = Math.min(limit, MAX_LIMIT);

    UUID lastId = parseLastIdFromCursor(cursor);
    Long lastLikeCount =
        sortBy == CommentSortBy.LIKE_COUNT && cursor != null ? parseLikeCountFromCursor(cursor)
            : null;
    LocalDateTime lastCreatedAt =
        sortBy == CommentSortBy.CREATED_AT && cursor != null ? parseCreatedAtFromCursor(cursor)
            : after;

    long totalCount = commentRepository.countByArticleIdAndDeletedAtIsNull(articleId);

    CommentSliceResult result = getCommentsByArticleCursor(articleId, sortBy.name(), sortDirection.name(),
        lastCreatedAt, lastLikeCount, lastId, safeLimit);

    List<CommentResponse> responses = result.content().stream()
        .map(c -> CommentResponse.of(c,
            commentLikeRepository.existsByCommentIdAndUserId(c.getId(), userId)))
        .toList();

    return new CursorPageResponse<>(responses, result.nextCursor(), result.nextAfter(),
        result.size(), (int) totalCount, result.hasNext());
  }


  // 커서 파싱 메서드들
  private UUID parseLastIdFromCursor(String cursor) {
    if (cursor == null || !cursor.contains(":"))
      return null;
    try {
      return UUID.fromString(cursor.substring(cursor.lastIndexOf(":") + 1));
    } catch (Exception e) {
      return null;
    }
  }

  private Long parseLikeCountFromCursor(String cursor) {
    if (cursor == null || !cursor.contains(":"))
      return null;
    try {
      return Long.parseLong(cursor.substring(0, cursor.lastIndexOf(":")));
    } catch (Exception e) {
      return null;
    }
  }

  private LocalDateTime parseCreatedAtFromCursor(String cursor) {
    if (cursor == null || !cursor.contains(":"))
      return null;
    try {
      return LocalDateTime.parse(cursor.substring(0, cursor.lastIndexOf(":")));
    } catch (Exception e) {
      return null;
    }
  }
}