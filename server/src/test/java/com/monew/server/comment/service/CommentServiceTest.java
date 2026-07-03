package com.monew.server.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.never;

import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.entity.CommentLike;
import com.monew.server.comment.repository.CommentLikeRepository;
import com.monew.server.comment.repository.CommentRepository;
import com.monew.server.common.exception.BaseException;
import com.monew.server.common.exception.CommonErrorCode;
import com.monew.server.notification.service.NotificationService;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock private CommentRepository commentRepository;
  @Mock private CommentLikeRepository commentLikeRepository;
  @Mock private ArticleRepository articleRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationService notificationService;

  @InjectMocks private CommentService commentService;

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
    // 핵심 검증: articleRepository.findActiveByIdForUpdate(비관적 락) 같은
    // 사전 존재/활성 여부 확인 없이도 예외 없이 삭제가 성공해야 한다.
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

  // 헬퍼 메서드 추가
  private CommentLike commentLike(UUID id, Comment comment, User user) {
    CommentLike commentLike = CommentLike.builder()
        .comment(comment)
        .user(user)
        .build();
    ReflectionTestUtils.setField(commentLike, "id", id);
    return commentLike;
  }

}