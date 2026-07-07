package com.monew.server.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.monew.server.activity.event.ArticleCommentCountUpdatedEvent;
import com.monew.server.activity.event.CommentCreatedEvent;
import com.monew.server.activity.event.CommentUpdatedEvent;
import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.dto.CommentCreateRequest;
import com.monew.server.comment.dto.CommentLikeResponse;
import com.monew.server.comment.dto.CommentResponse;
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
import com.monew.server.notification.service.NotificationService;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;
  @Mock
  private CommentLikeRepository commentLikeRepository;
  @Mock
  private ArticleRepository articleRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private CommentService commentService;

  @Test
  @DisplayName("댓글 등록 성공 - 유효한 요청이면 댓글을 저장하고 카운트 증가 및 이벤트를 발행한다")
  void createComment_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    User user = user(userId);
    Article article = article(articleId);
    CommentCreateRequest request = new CommentCreateRequest(articleId, "새 댓글");

    given(articleRepository.findByIdAndDeletedAtIsNull(articleId))
        .willReturn(Optional.of(article));
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.of(user));
    given(commentRepository.save(any(Comment.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    Comment result = commentService.createComment(request, userId);

    // then
    assertThat(result.getContent()).isEqualTo("새 댓글");
    assertThat(result.getUser()).isEqualTo(user);
    assertThat(result.getArticle()).isEqualTo(article);

    then(commentRepository).should().save(any(Comment.class));
    then(articleRepository).should().increaseCommentCount(articleId);
    then(eventPublisher).should().publishEvent(any(CommentCreatedEvent.class));
    then(eventPublisher).should().publishEvent(any(ArticleCommentCountUpdatedEvent.class));
  }

  @Test
  @DisplayName("댓글 등록 응답 성공 - 등록된 댓글 정보로 응답을 조립한다")
  void createCommentResponse_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    User user = user(userId);
    Article article = article(articleId);
    CommentCreateRequest request = new CommentCreateRequest(articleId, "새 댓글");

    given(articleRepository.findByIdAndDeletedAtIsNull(articleId))
        .willReturn(Optional.of(article));
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.of(user));
    given(commentRepository.save(any(Comment.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    CommentResponse response = commentService.createCommentResponse(request, userId);

    // then
    assertThat(response.content()).isEqualTo("새 댓글");
    assertThat(response.likedByMe()).isFalse();
  }

  @Test
  @DisplayName("댓글 등록 실패 - 기사가 없으면 기사 없음 예외가 발생하고 이벤트는 발행되지 않는다")
  void createComment_fail_articleNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(articleId, "새 댓글");

    given(articleRepository.findByIdAndDeletedAtIsNull(articleId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.createComment(request, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ArticleErrorCode.ARTICLE_NOT_FOUND);

    then(userRepository).should(never()).findByIdAndDeletedAtIsNull(any());
    then(commentRepository).should(never()).save(any());
    then(eventPublisher).should(never()).publishEvent(any());
  }

  @Test
  @DisplayName("댓글 등록 실패 - 사용자가 없으면 사용자 없음 예외가 발생하고 이벤트는 발행되지 않는다")
  void createComment_fail_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    Article article = article(articleId);
    CommentCreateRequest request = new CommentCreateRequest(articleId, "새 댓글");

    given(articleRepository.findByIdAndDeletedAtIsNull(articleId))
        .willReturn(Optional.of(article));
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.createComment(request, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);

    then(commentRepository).should(never()).save(any());
    then(articleRepository).should(never()).increaseCommentCount(any());
    then(eventPublisher).should(never()).publishEvent(any());
  }


  @Test
  @DisplayName("댓글 수정 성공 - 본인 댓글이면 내용을 수정하고 이벤트를 발행한다")
  void updateComment_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    User user = user(userId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, user, "원래 내용");
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));

    // when
    Comment result = commentService.updateComment(commentId, userId, request);

    // then
    assertThat(result.getContent()).isEqualTo("수정된 내용");
    then(eventPublisher).should().publishEvent(any(CommentUpdatedEvent.class));
  }

  @Test
  @DisplayName("댓글 수정 응답 성공 - 수정된 댓글 정보와 좋아요 여부로 응답을 조립한다")
  void updateCommentResponse_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    User user = user(userId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, user, "원래 내용");
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));
    given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId))
        .willReturn(true);

    // when
    CommentResponse response = commentService.updateCommentResponse(commentId, userId, request);

    // then
    assertThat(response.content()).isEqualTo("수정된 내용");
    assertThat(response.likedByMe()).isTrue();
  }

  @Test
  @DisplayName("댓글 수정 실패 - 본인이 작성한 댓글이 아니면 권한 없음 예외가 발생하고 이벤트는 발행되지 않는다")
  void updateComment_fail_notOwner() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    User owner = user(ownerId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, owner, "원래 내용");
    CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> commentService.updateComment(commentId, otherUserId, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.FORBIDDEN);

    assertThat(comment.getContent()).isEqualTo("원래 내용"); // 내용이 변경되지 않았는지 확인
    then(eventPublisher).should(never()).publishEvent(any());
  }

  @Test
  @DisplayName("댓글 수정 실패 - 댓글이 존재하지 않으면 댓글 없음 예외가 발생한다")
  void updateComment_fail_commentNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.updateComment(commentId, userId, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);

    then(eventPublisher).should(never()).publishEvent(any());
  }


  @Test
  @DisplayName("댓글 삭제 성공 - 기사가 논리 삭제된 상태여도 댓글은 삭제된다")
  void deleteComment_success_evenIfArticleDeleted() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    User user = user(userId);
    Article article = article(articleId);
    Comment comment = comment(commentId, article, user, "삭제될 댓글");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));

    // when & then
    assertThatCode(() -> commentService.deleteComment(commentId, userId))
        .doesNotThrowAnyException();

    assertThat(comment.getDeletedAt()).isNotNull();
    then(articleRepository).should().decreaseCommentCount(articleId);
  }

  @Test
  @DisplayName("댓글 삭제 실패 - 본인이 작성한 댓글이 아니면 권한 없음 예외가 발생한다")
  void deleteComment_fail_notOwner() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    User owner = user(ownerId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, owner, "타인이 지우려는 댓글");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> commentService.deleteComment(commentId, otherUserId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.FORBIDDEN);

    then(articleRepository).should(never()).decreaseCommentCount(any());
  }

  @Test
  @DisplayName("댓글 삭제 실패 - 이미 삭제된 댓글이면 잘못된 요청 예외가 발생한다")
  void deleteComment_fail_alreadyDeleted() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    User user = user(userId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, user, "이미 삭제된 댓글");
    comment.delete(); // 미리 삭제 상태로 만들어둠

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> commentService.deleteComment(commentId, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.INVALID_REQUEST);
  }

  @Test
  @DisplayName("댓글 삭제 실패 - 댓글이 존재하지 않으면 댓글 없음 예외가 발생한다")
  void deleteComment_fail_commentNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.deleteComment(commentId, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);

    then(articleRepository).should(never()).decreaseCommentCount(any());
  }


  @Test
  @DisplayName("댓글 단건 조회 응답 성공 - 좋아요 여부를 포함해 응답을 조립한다")
  void getCommentResponse_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    Article article = article(UUID.randomUUID());
    User user = user(UUID.randomUUID());
    Comment comment = comment(commentId, article, user, "댓글 내용");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));
    given(commentLikeRepository.existsByCommentIdAndUserId(commentId, userId))
        .willReturn(true);

    // when
    CommentResponse response = commentService.getCommentResponse(commentId, userId);

    // then
    assertThat(response.id()).isEqualTo(commentId);
    assertThat(response.likedByMe()).isTrue();
  }

  @Test
  @DisplayName("댓글 단건 조회 실패 - 댓글이 존재하지 않으면 댓글 없음 예외가 발생한다")
  void getComment_fail_commentNotFound() {
    // given
    UUID commentId = UUID.randomUUID();

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.getComment(commentId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
  }


  @Test
  @DisplayName("좋아요 등록 성공 - 처음 누르는 좋아요면 카운트를 증가시키고 알림을 생성한다")
  void addLike_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID commentOwnerId = UUID.randomUUID();

    User liker = user(userId);
    User commentOwner = user(commentOwnerId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, commentOwner, "좋아요 대상 댓글");
    CommentLike commentLike = commentLike(UUID.randomUUID(), comment, liker);

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.of(liker));
    given(commentLikeRepository.insertIgnore(any(UUID.class), eq(commentId), eq(userId)))
        .willReturn(1);
    given(commentLikeRepository.findByCommentIdAndUserId(commentId, userId))
        .willReturn(Optional.of(commentLike));

    // when
    CommentLike result = commentService.addLike(commentId, userId);

    // then
    assertThat(result).isEqualTo(commentLike);
    then(commentRepository).should().increaseLikeCount(commentId);
    then(notificationService).should().createCommentLikeNotification(
        commentOwnerId, userId, commentId, liker.getNickname());
  }

  @Test
  @DisplayName("좋아요 등록 실패 - 이미 좋아요 상태면 카운트 증가와 알림 생성 없이 잘못된 요청 예외가 발생한다")
  void addLike_fail_alreadyLiked() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    User liker = user(userId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, user(UUID.randomUUID()), "댓글");

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.of(liker));
    // insertIgnore가 0을 반환 = UNIQUE 제약 충돌 = 이미 좋아요 상태
    given(commentLikeRepository.insertIgnore(any(UUID.class), eq(commentId), eq(userId)))
        .willReturn(0);

    // when & then
    assertThatThrownBy(() -> commentService.addLike(commentId, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.INVALID_REQUEST);

    // 핵심 검증: 예외가 발생하면 카운트 증가와 알림 생성은 절대 호출되면 안 된다
    then(commentRepository).should(never()).increaseLikeCount(any());
    then(notificationService).should(never())
        .createCommentLikeNotification(any(), any(), any(), any());
  }

  @Test
  @DisplayName("좋아요 등록 실패 - 댓글이 존재하지 않으면 댓글 없음 예외가 발생한다")
  void addLike_fail_commentNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.addLike(commentId, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);

    then(commentLikeRepository).should(never()).insertIgnore(any(), any(), any());
  }

  @Test
  @DisplayName("좋아요 등록 응답 성공 - 좋아요 등록 후 갱신된 정보로 응답을 조립한다")
  void addLikeAndGetResponse_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID commentOwnerId = UUID.randomUUID();

    User liker = user(userId);
    User commentOwner = user(commentOwnerId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, commentOwner, "좋아요 대상");
    CommentLike commentLike = commentLike(UUID.randomUUID(), comment, liker);

    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.of(liker));
    given(commentLikeRepository.insertIgnore(any(UUID.class), eq(commentId), eq(userId)))
        .willReturn(1);
    given(commentLikeRepository.findByCommentIdAndUserId(commentId, userId))
        .willReturn(Optional.of(commentLike));

    // when
    CommentLikeResponse response = commentService.addLikeAndGetResponse(commentId, userId);

    // then
    assertThat(response.commentId()).isEqualTo(commentId);
    assertThat(response.likedBy()).isEqualTo(userId);
  }


  @Test
  @DisplayName("좋아요 취소 성공 - 좋아요 상태이면 삭제하고 카운트를 감소시킨다")
  void removeLike_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    User liker = user(userId);
    Article article = article(UUID.randomUUID());
    Comment comment = comment(commentId, article, user(UUID.randomUUID()), "댓글");
    CommentLike commentLike = commentLike(UUID.randomUUID(), comment, liker);

    given(commentLikeRepository.findByCommentIdAndUserId(commentId, userId))
        .willReturn(Optional.of(commentLike));
    given(commentRepository.findByIdAndDeletedAtIsNull(commentId))
        .willReturn(Optional.of(comment));

    // when
    commentService.removeLike(commentId, userId);

    // then
    then(commentLikeRepository).should().delete(commentLike);
    then(commentRepository).should().decreaseLikeCount(commentId);
  }

  @Test
  @DisplayName("좋아요 취소 실패 - 좋아요를 누르지 않은 상태면 잘못된 요청 예외가 발생한다")
  void removeLike_fail_notLiked() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    given(commentLikeRepository.findByCommentIdAndUserId(commentId, userId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> commentService.removeLike(commentId, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.INVALID_REQUEST);

    then(commentRepository).should(never()).decreaseLikeCount(any());
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - 조회 결과가 있으면 다음 커서를 생성한다")
  void getComments_success_generatesNextCursor() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Comment comment = comment(UUID.randomUUID(), article(UUID.randomUUID()), user(UUID.randomUUID()), "댓글");
    ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now()); // [추가] nextCursor 생성에 필요

    given(commentRepository.countByArticleIdAndDeletedAtIsNull(articleId))
        .willReturn(1L);
    given(commentRepository.findCommentsByArticleValueCursor(
        any(), any(), any(), any(), any()))
        .willReturn(List.of(comment));
    given(commentLikeRepository.existsByCommentIdAndUserId(any(), any()))
        .willReturn(false);

    // when
    var result = commentService.getComments(articleId, "createdAt", "DESC", null, null, 10, userId);

    // then
    assertThat(result.content()).hasSize(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - limit이 최대값(50)을 초과하면 50으로 캡핑된다")
  void getComments_success_limitCappedAtMax() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    int requestedLimit = 1000; // 상한을 훨씬 초과하는 값

    given(commentRepository.countByArticleIdAndDeletedAtIsNull(articleId))
        .willReturn(0L);
    given(commentRepository.findCommentsByArticleValueCursor(
        any(), any(), any(), any(), any()))
        .willReturn(List.of());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    // when
    commentService.getComments(articleId, "createdAt", "DESC",
        null, null, requestedLimit, userId);

    // then
    then(commentRepository).should().findCommentsByArticleValueCursor(
        any(), any(), any(), any(), pageableCaptor.capture());

    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(51); // MAX_LIMIT(50) + 1
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - limit이 최대값 이하이면 그대로 사용된다")
  void getComments_success_limitWithinMax() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    int requestedLimit = 10;

    given(commentRepository.countByArticleIdAndDeletedAtIsNull(articleId))
        .willReturn(0L);
    given(commentRepository.findCommentsByArticleValueCursor(
        any(), any(), any(), any(), any()))
        .willReturn(List.of());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    // when
    commentService.getComments(articleId, "createdAt", "DESC",
        null, null, requestedLimit, userId);

    // then
    then(commentRepository).should().findCommentsByArticleValueCursor(
        any(), any(), any(), any(), pageableCaptor.capture());

    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(11); // 10 + 1
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - 좋아요 순 정렬이면 findCommentsByArticleLikeCursor를 호출한다")
  void getComments_success_sortByLikeCount() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    given(commentRepository.countByArticleIdAndDeletedAtIsNull(articleId))
        .willReturn(0L);
    given(commentRepository.findCommentsByArticleLikeCursor(
        any(), any(), any(), any(), any()))
        .willReturn(List.of());

    // when
    // cursor="5:{uuid}" 형태 — parseLikeCountFromCursor, parseLastIdFromCursor 둘 다 실행
    UUID lastId = UUID.randomUUID();
    commentService.getComments(articleId, "likeCount", "DESC",
        "5:" + lastId, null, 10, userId);

    // then
    then(commentRepository).should().findCommentsByArticleLikeCursor(
        eq(articleId), eq(5L), eq(lastId), eq("DESC"), any());
    then(commentRepository).should(never()).findCommentsByArticleValueCursor(
        any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("댓글 목록 조회 성공 - 작성일 순 정렬이면서 cursor가 있으면 lastCreatedAt을 파싱한다")
  void getComments_success_sortByCreatedAtWithCursor() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 10, 0);
    UUID lastId = UUID.randomUUID();

    given(commentRepository.countByArticleIdAndDeletedAtIsNull(articleId))
        .willReturn(0L);
    given(commentRepository.findCommentsByArticleValueCursor(
        any(), any(), any(), any(), any()))
        .willReturn(List.of());

    // when
    // parseCreatedAtFromCursor가 실제로 날짜 문자열을 파싱하도록 유도
    commentService.getComments(articleId, "createdAt", "DESC",
        createdAt + ":" + lastId, null, 10, userId);

    // then
    then(commentRepository).should().findCommentsByArticleValueCursor(
        eq(articleId), eq(createdAt), eq(lastId), eq("DESC"), any());
  }

  @Test
  @DisplayName("댓글 목록 조회 실패 - 지원하지 않는 정렬 기준이면 잘못된 요청 예외가 발생한다")
  void getComments_fail_invalidOrderBy() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when & then
    //orderBy에서 "asdf"는 더미값
    assertThatThrownBy(() ->
        commentService.getComments(articleId, "asdf", "DESC",
            null, null, 10, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.INVALID_REQUEST);

    // 파라미터 검증에서 막혔으므로, DB 조회 자체가 시도되면 안 된다
    then(commentRepository).should(never()).countByArticleIdAndDeletedAtIsNull(any());
  }

  @Test
  @DisplayName("댓글 목록 조회 실패 - 지원하지 않는 정렬 방향이면 잘못된 요청 예외가 발생한다")
  void getComments_fail_invalidDirection() {
    // given
    UUID articleId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    // when & then
    //direction에서 "UP"은 더미값. ASC 아니면 DESC로 존재해야 함
    assertThatThrownBy(() ->
        commentService.getComments(articleId, "createdAt", "UP",
            null, null, 10, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.INVALID_REQUEST);

    then(commentRepository).should(never()).countByArticleIdAndDeletedAtIsNull(any());
  }


  private User user(UUID userId) {
    User user = new User(userId + "@monew.com", "테스터", "hashed-pw");
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }

  private Article article(UUID articleId) {
    Article article = new Article();
    ReflectionTestUtils.setField(article, "id", articleId);
    ReflectionTestUtils.setField(article, "source", ArticleSource.NAVER);
    ReflectionTestUtils.setField(article, "sourceUrl", "https://news.monew.test/" + articleId);
    ReflectionTestUtils.setField(article, "title", "테스트 기사");
    ReflectionTestUtils.setField(article, "publishDate", LocalDateTime.now());
    return article;
  }

  private Comment comment(UUID commentId, Article article, User user, String content) {
    Comment comment = Comment.builder()
        .article(article)
        .user(user)
        .content(content)
        .build();
    ReflectionTestUtils.setField(comment, "id", commentId);
    ReflectionTestUtils.setField(comment, "likeCount", 0L);
    return comment;
  }

  private CommentLike commentLike(UUID id, Comment comment, User user) {
    CommentLike commentLike = CommentLike.builder()
        .comment(comment)
        .user(user)
        .build();
    ReflectionTestUtils.setField(commentLike, "id", id);
    return commentLike;
  }
}