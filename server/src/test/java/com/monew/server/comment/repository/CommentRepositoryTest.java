package com.monew.server.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.monew.server.article.entity.Article;
import com.monew.server.article.entity.ArticleSource;
import com.monew.server.article.repository.ArticleRepository;
import com.monew.server.comment.entity.Comment;
import com.monew.server.support.RepositoryTestSupport;
import com.monew.server.user.entity.User;
import com.monew.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class CommentRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private UserRepository userRepository;


  @Test
  @DisplayName("작성일 커서 조회 성공 - 최초 조회(cursor 없음)면 최신순으로 댓글을 가져온다")
  void findCommentsByArticleValueCursor_success_firstPage() throws InterruptedException {
    // given
    User user = userRepository
                  .save(new User("test1@monew.com", "테스터1", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/1"));

    commentRepository.save(commentOf(article, user, "첫 댓글"));
    Thread.sleep(10);
    commentRepository.save(commentOf(article, user, "둘째 댓글"));
    Thread.sleep(10);
    Comment third = commentRepository.save(commentOf(article, user, "셋째 댓글"));

    // when
    List<Comment> result = commentRepository.findCommentsByArticleValueCursor(
        article.getId(), null, null, "DESC",
        PageRequest.of(0, 10));

    // then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getId()).isEqualTo(third.getId());
  }


  @Test
  @DisplayName("작성일 커서 조회 성공 - cursor 이후 데이터만 가져온다")
  void findCommentsByArticleValueCursor_success_nextPage() throws InterruptedException {
    // given
    User user = userRepository
                  .save(new User("test2@monew.com", "테스터2", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/2"));

    Comment first = commentRepository.save(commentOf(article, user, "첫 댓글"));
    Thread.sleep(10);
    Comment second = commentRepository.save(commentOf(article, user, "둘째 댓글"));
    Comment savedFirst = commentRepository.findById(first.getId()).orElseThrow();

    // when
    List<Comment> result = commentRepository.findCommentsByArticleValueCursor(
        article.getId(), savedFirst.getCreatedAt(), savedFirst.getId(), "ASC",
        PageRequest.of(0, 10));

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(second.getId());
  }


  @Test
  @DisplayName("좋아요 순 커서 조회 성공 - likeCount 내림차순으로 댓글을 가져온다")
  void findCommentsByArticleLikeCursor_success() {
    // given
    User user = userRepository
                  .save(new User("test3@monew.com", "테스터3", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/3"));

    Comment low = commentRepository.save(commentOf(article, user, "좋아요 적은 댓글"));
    Comment high = commentRepository.save(commentOf(article, user, "좋아요 많은 댓글"));
    commentRepository.increaseLikeCount(high.getId());
    commentRepository.increaseLikeCount(high.getId()); // high는 2, low는 0

    // when
    List<Comment> result = commentRepository.findCommentsByArticleLikeCursor(
        article.getId(), null, null, "DESC",
        PageRequest.of(0, 10));

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(high.getId()); // 좋아요 많은 게 먼저
  }


  @Test
  @DisplayName("좋아요 수 원자적 증감 성공 - increaseLikeCount 호출 시 정확히 1 증가한다")
  void increaseLikeCount_success() {
    // given
    User user = userRepository
                  .save(new User("test4@monew.com", "테스터4", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/4"));
    Comment comment = commentRepository.save(commentOf(article, user, "좋아요 대상"));

    // when
    commentRepository.increaseLikeCount(comment.getId());

    // then
    Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
    assertThat(updated.getLikeCount()).isEqualTo(1L);
  }


  @Test
  @DisplayName("좋아요 수 원자적 감소 성공 - 0 이하로는 감소하지 않는다")
  void decreaseLikeCount_success_notBelowZero() {
    // given
    User user = userRepository
                  .save(new User("test5@monew.com", "테스터5", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/5"));
    Comment comment = commentRepository.save(commentOf(article, user, "좋아요 0인 댓글"));

    // when
    commentRepository.decreaseLikeCount(comment.getId()); // likeCount가 이미 0

    // then
    Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
    assertThat(updated.getLikeCount()).isZero();
  }


  // 기본 생성자 + ReflectionTestUtils로 필드를 채워서 저장 가능한 상태로 만든다(ArticleServiceTest와 통일)
  private Article article(String sourceUrl) {
    Article article = new Article();
    ReflectionTestUtils.setField(article, "id", java.util.UUID.randomUUID());
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