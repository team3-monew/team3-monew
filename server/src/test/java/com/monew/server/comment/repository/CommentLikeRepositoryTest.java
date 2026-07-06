package com.monew.server.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.entity.Comment;
import com.monew.server.comment.entity.CommentLike;
import com.monew.server.support.RepositoryTestSupport;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class CommentLikeRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private CommentLikeRepository commentLikeRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;


  @Test
  @DisplayName("좋아요 삽입 성공 - 처음 누르는 좋아요면 정상적으로 저장되고 1을 반환한다")
  void insertIgnore_success_firstLike() {
    // given
    User user = userRepository
                  .save(new User("like1@monew.com", "테스터1", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/like1"));
    Comment comment = commentRepository.save(commentOf(article, user, "좋아요 대상"));

    // when
    int inserted = commentLikeRepository.insertIgnore(UUID.randomUUID(), comment.getId(), user.getId());

    // then
    assertThat(inserted).isEqualTo(1);
    Optional<CommentLike> saved = commentLikeRepository
                                    .findByCommentIdAndUserId(comment.getId(), user.getId());
    assertThat(saved).isPresent();
  }


  @Test
  @DisplayName("좋아요 삽입 실패 - 이미 좋아요 상태면 UNIQUE 제약 충돌로 0을 반환하고 예외는 발생하지 않는다")
  void insertIgnore_success_duplicateReturnsZero() {
    // given
    User user = userRepository
                  .save(new User("like2@monew.com", "테스터2", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/like2"));
    Comment comment = commentRepository.save(commentOf(article, user, "좋아요 대상"));

    // 먼저 한 번 좋아요를 등록해둠
    commentLikeRepository.insertIgnore(UUID.randomUUID(), comment.getId(), user.getId());

    // when
    // [핵심] 같은 comment_id, user_id 조합으로 다시 INSERT 시도
    // ON CONFLICT DO NOTHING이 실제로 동작해서 예외 없이 0을 반환하는지가 검증 포인트
    int secondInserted = commentLikeRepository
                          .insertIgnore(UUID.randomUUID(), comment.getId(), user.getId());

    // then
    assertThat(secondInserted).isZero();
  }


  @Test
  @DisplayName("좋아요 삽입 성공 - 서로 다른 사용자면 같은 댓글에도 각각 저장된다")
  void insertIgnore_success_differentUsersOnSameComment() {
    // given
    User user1 = userRepository.save(new User("like3@monew.com", "테스터3", "hashed-pw"));
    User user2 = userRepository.save(new User("like4@monew.com", "테스터4", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/like3"));
    Comment comment = commentRepository.save(commentOf(article, user1, "좋아요 대상"));

    // when
    int firstResult = commentLikeRepository.insertIgnore(UUID.randomUUID(), comment.getId(), user1.getId());
    int secondResult = commentLikeRepository.insertIgnore(UUID.randomUUID(), comment.getId(), user2.getId());

    // then
    assertThat(firstResult).isEqualTo(1);
    assertThat(secondResult).isEqualTo(1);
  }


  @Test
  @DisplayName("좋아요 존재 확인 성공 - 좋아요 상태이면 true를 반환한다")
  void existsByCommentIdAndUserId_success_true() {
    // given
    User user = userRepository
                  .save(new User("like5@monew.com", "테스터5", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/like5"));
    Comment comment = commentRepository.save(commentOf(article, user, "좋아요 대상"));
    commentLikeRepository.insertIgnore(UUID.randomUUID(), comment.getId(), user.getId());

    // when
    boolean exists = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId());

    // then
    assertThat(exists).isTrue();
  }


  @Test
  @DisplayName("좋아요 존재 확인 성공 - 좋아요 하지 않은 상태이면 false를 반환한다")
  void existsByCommentIdAndUserId_success_false() {
    // given
    User user = userRepository
                  .save(new User("like6@monew.com", "테스터6", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/like6"));
    Comment comment = commentRepository.save(commentOf(article, user, "좋아요 안 누른 대상"));

    // when
    boolean exists = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), user.getId());

    // then
    assertThat(exists).isFalse();
  }


  private Article article(String sourceUrl) {
    Article article = new Article();
    ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(article, "source", ArticleSource.NAVER);
    ReflectionTestUtils.setField(article, "sourceUrl", sourceUrl);
    ReflectionTestUtils.setField(article, "title", "테스트 기사");
    ReflectionTestUtils.setField(article, "publishDate", LocalDateTime.now());
    ReflectionTestUtils.setField(article, "commentCount", 0L);
    ReflectionTestUtils.setField(article, "viewCount", 0L);
    return article;
  }


  private Comment commentOf(Article article, User user, String content) {
    return Comment.builder()
        .article(article)
        .user(user)
        .content(content)
        .build();
  }
}