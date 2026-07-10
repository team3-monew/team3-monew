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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        article.getId(), null, null, null, "DESC",
        PageRequest.of(0, 10));

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(high.getId()); // 좋아요 많은 게 먼저
  }

  @Test
  @DisplayName("좋아요 순 커서 조회 성공 - 좋아요 수가 같으면 direction과 무관하게 등록순(오래된 게 먼저)으로 정렬된다")
  void findCommentsByArticleLikeCursor_success_tieBreakByCreatedAt() throws InterruptedException {
    // given
    User user = userRepository.save(new User("tiebreak@monew.com", "테스터", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/tiebreak"));
    Comment first = commentRepository.save(commentOf(article, user, "먼저 쓴 댓글"));
    Thread.sleep(10);
    Comment second = commentRepository.save(commentOf(article, user, "나중에 쓴 댓글"));

    // when — 인자 순서: articleId, lastLikeCount, lastCreatedAt, lastId, direction, pageable
    List<Comment> result = commentRepository.findCommentsByArticleLikeCursor(
            article.getId(), null, null, null, "DESC", PageRequest.of(0, 10));

    // then
    // 좋아요 수가 둘 다 0으로 동점 → direction(DESC)과 무관하게 오래된 댓글이 먼저 나와야 함
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getId()).isEqualTo(first.getId());
    assertThat(result.get(1).getId()).isEqualTo(second.getId());
  }

  @Test
  @DisplayName("좋아요 순 커서 조회 성공 - direction=ASC여도 동점자는 여전히 등록순(오래된 게 먼저)")
  void findCommentsByArticleLikeCursor_success_tieBreakByCreatedAt_regardlessOfDirection()
          throws InterruptedException {
    User user = userRepository.save(new User("tiebreak-asc@monew.com", "테스터", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/tiebreak-asc"));
    Comment first = commentRepository.save(commentOf(article, user, "먼저 쓴 댓글"));
    Thread.sleep(10);
    Comment second = commentRepository.save(commentOf(article, user, "나중에 쓴 댓글"));

    List<Comment> result = commentRepository.findCommentsByArticleLikeCursor(
            article.getId(), null, null, null, "ASC", PageRequest.of(0, 10));

    assertThat(result.get(0).getId()).isEqualTo(first.getId());
    assertThat(result.get(1).getId()).isEqualTo(second.getId());
  }

  @Test
  @DisplayName("좋아요 순 커서 조회 성공 - 좋아요 그룹과 동점자 그룹이 섞여 있어도 순서가 맞는다")
  void findCommentsByArticleLikeCursor_success_mixedLikeGroups() throws InterruptedException {
    // given
    User user = userRepository.save(new User("mixed@monew.com", "테스터", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/mixed"));

    Comment c111 = commentRepository.save(commentOf(article, user, "111"));
    Thread.sleep(10);
    Comment c222 = commentRepository.save(commentOf(article, user, "222"));
    Thread.sleep(10);
    Comment c333 = commentRepository.save(commentOf(article, user, "333"));
    Thread.sleep(10);
    Comment c444 = commentRepository.save(commentOf(article, user, "444"));
    Thread.sleep(10);
    Comment c555 = commentRepository.save(commentOf(article, user, "555"));

    // 222, 333에 좋아요 1개씩 부여
    commentRepository.increaseLikeCount(c222.getId());
    commentRepository.increaseLikeCount(c333.getId());

    // when
    List<Comment> result = commentRepository.findCommentsByArticleLikeCursor(
            article.getId(), null, null, null, "DESC", PageRequest.of(0, 10));

    // then
    // 좋아요 그룹(222,333)이 먼저 나오고, 그 안에서는 오래된 222가 위
    // 그 다음 좋아요 없는 그룹(111,444,555)도 오래된 순
    assertThat(result).extracting(Comment::getContent)
            .containsExactly("222", "333", "111", "444", "555");
  }

  @Test
  @DisplayName("좋아요 순 커서 페이지네이션 - 동점자 그룹이 페이지 경계에 걸쳐도 누락/중복 없이 조회된다")
  void findCommentsByArticleLikeCursor_success_pagingAcrossTieBoundary() throws InterruptedException {
    // given: 좋아요 수가 모두 0으로 동점인 댓글 5개를 등록순으로 생성
    User user = userRepository.save(new User("paging@monew.com", "테스터", "hashed-pw"));
    Article article = articleRepository.save(article("https://news.monew.test/paging"));

    List<Comment> saved = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      saved.add(commentRepository.save(commentOf(article, user, "댓글" + i)));
      Thread.sleep(10);
    }

    // when: 페이지 크기 2로 끊어서 커서 페이지네이션을 반복 호출
    // (CommentService가 하는 것과 동일하게, 매 페이지의 마지막 댓글 값을 다음 요청의 커서로 사용)
    List<Comment> collected = new ArrayList<>();
    Long lastLikeCount = null;
    LocalDateTime lastCreatedAt = null;
    UUID lastId = null;

    for (int page = 0; page < 3; page++) {
      List<Comment> pageResult = commentRepository.findCommentsByArticleLikeCursor(
              article.getId(), lastLikeCount, lastCreatedAt, lastId, "DESC", PageRequest.of(0, 2));

      if (pageResult.isEmpty()) {
        break;
      }

      collected.addAll(pageResult);
      Comment last = pageResult.get(pageResult.size() - 1);
      lastLikeCount = last.getLikeCount();
      lastCreatedAt = last.getCreatedAt();
      lastId = last.getId();
    }

    // then: 전체 5개가 누락/중복 없이, 등록순(오래된 게 먼저) 그대로 수집되어야 함
    assertThat(collected).hasSize(5);
    assertThat(collected).extracting(Comment::getId)
            .containsExactlyElementsOf(saved.stream().map(Comment::getId).toList());
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