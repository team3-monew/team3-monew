package com.monew.batch.article.collect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.monew.batch.article.collect.collector.FeedBasedArticleCollector;
import com.monew.batch.article.collect.collector.KeywordBasedArticleCollector;
import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.dto.KeywordCollectResultDto;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.collect.dto.ArticleCollectStagingCleanupResultDto;
import com.monew.batch.article.collect.dto.ArticleCollectStepResultDto;
import com.monew.batch.article.collect.dto.ArticleSaveAndInterestLinkStepResultDto;
import com.monew.batch.article.collect.entity.ArticleCollectStaging;
import com.monew.batch.article.collect.repository.ArticleCollectStagingRepository;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.entity.Article;
import com.monew.batch.article.entity.ArticleInterest;
import com.monew.batch.article.entity.ArticleInterestId;
import com.monew.batch.article.entity.ArticleSource;
import com.monew.batch.article.event.InterestMatchedArticleEvent;
import com.monew.batch.article.repository.ArticleInterestRepository;
import com.monew.batch.article.repository.ArticleRepository;
import com.monew.batch.common.event.BatchEventPublisher;
import com.monew.batch.interest.entity.Interest;
import com.monew.batch.interest.entity.InterestKeyword;
import com.monew.batch.interest.repository.InterestKeywordRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArticleCollectServiceTest {

  private static final long JOB_INSTANCE_ID = 1L;
  private static final LocalDateTime PUBLISH_DATE = LocalDateTime.of(2026, 7, 3, 10, 0);

  @Mock
  KeywordBasedArticleCollector keywordCollector;

  @Mock
  FeedBasedArticleCollector feedCollector;

  @Mock
  InterestKeywordRepository interestKeywordRepository;

  @Mock
  ArticleRepository articleRepository;

  @Mock
  ArticleInterestRepository articleInterestRepository;

  @Mock
  ArticleCollectStagingRepository stagingRepository;

  @Mock
  BatchEventPublisher batchEventPublisher;

  ArticleCollectService articleCollectService;

  @BeforeEach
  void setUp() {
    articleCollectService = service(List.of(keywordCollector), List.of(feedCollector));
  }

  @Test
  @DisplayName("성공 - 관심사 키워드가 없으면 Naver 기사 수집을 스킵한다")
  void collectNaverArticlesToStaging_success_skipWhenNoInterestKeywords() {
    // given
    given(interestKeywordRepository.findAll()).willReturn(List.of());

    // when
    ArticleCollectStepResultDto result =
        articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID);

    // then
    assertThat(result).isEqualTo(ArticleCollectStepResultDto.empty(ArticleSource.NAVER));
    then(keywordCollector).should(never()).collectByKeywordResult(any(), any(Integer.class));
    then(stagingRepository).should(never()).saveAll(any());
  }

  @Test
  @DisplayName("성공 - 키워드를 정규화해서 같은 키워드는 한 번만 요청한다")
  void collectNaverArticlesToStaging_success_requestOnceByNormalizedKeyword() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(List.of(
        interestKeyword(interest, " Java "),
        interestKeyword(interest, "java")
    ));
    given(keywordCollector.collectByKeywordResult("java", 100))
        .willReturn(new KeywordCollectResultDto(List.of(), 1, 1, 0));

    // when
    ArticleCollectStepResultDto result =
        articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID);

    // then
    assertThat(result.requestCount()).isEqualTo(1);
    then(keywordCollector).should(times(1)).collectByKeywordResult("java", 100);
    then(keywordCollector).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("성공 - 제목이나 요약에 관심사 키워드가 포함된 기사만 staging에 저장한다")
  void collectNaverArticlesToStaging_success_stageOnlyMatchedArticles() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(keywordCollector.collectByKeywordResult("java", 100))
        .willReturn(new KeywordCollectResultDto(List.of(
            collected("https://news.example.com/java", "Java 새 버전 공개", "요약", ArticleSource.NAVER),
            collected("https://news.example.com/sports", "야구 소식", "요약", ArticleSource.NAVER)
        ), 1, 1, 0));
    given(stagingRepository.findAllByJobInstanceIdAndSourceUrlIn(eq(JOB_INSTANCE_ID),
        anyCollection())).willReturn(List.of());

    // when
    ArticleCollectStepResultDto result =
        articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID);

    // then
    assertThat(result.collectedCount()).isEqualTo(2);
    assertThat(result.matchedCount()).isEqualTo(1);
    assertThat(result.stagedCount()).isEqualTo(1);

    ArgumentCaptor<Iterable<ArticleCollectStaging>> captor = stagingCaptor();
    then(stagingRepository).should().saveAll(captor.capture());
    assertThat(captor.getValue())
        .extracting(ArticleCollectStaging::getSourceUrl)
        .containsExactly("https://news.example.com/java");
  }

  @Test
  @DisplayName("성공 - 같은 sourceUrl 기사는 staging 저장 시 중복 제거한다")
  void collectNaverArticlesToStaging_success_deduplicateSameSourceUrlWhenStaging() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(keywordCollector.collectByKeywordResult("java", 100))
        .willReturn(new KeywordCollectResultDto(List.of(
            collected("https://news.example.com/java", "Java 첫 번째 기사", "요약", ArticleSource.NAVER),
            collected("https://news.example.com/java", "Java 두 번째 기사", "요약", ArticleSource.NAVER)
        ), 1, 1, 0));
    given(stagingRepository.findAllByJobInstanceIdAndSourceUrlIn(eq(JOB_INSTANCE_ID),
        anyCollection())).willReturn(List.of());

    // when
    ArticleCollectStepResultDto result =
        articleCollectService.collectNaverArticlesToStaging(JOB_INSTANCE_ID);

    // then
    assertThat(result.matchedCount()).isEqualTo(2);
    assertThat(result.stagedCount()).isEqualTo(1);
    assertThat(result.duplicateSkippedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공 - RSS source에 해당하는 collector가 없으면 수집을 스킵한다")
  void collectRssArticlesToStaging_success_skipWhenCollectorNotFound() {
    // given
    ArticleCollectService serviceWithoutFeedCollector = service(List.of(keywordCollector),
        List.of());
    given(interestKeywordRepository.findAll())
        .willReturn(List.of(interestKeyword(interest("경제"), "금리")));

    // when
    ArticleCollectStepResultDto result =
        serviceWithoutFeedCollector.collectRssArticlesToStaging(JOB_INSTANCE_ID,
            ArticleSource.HANKYUNG);

    // then
    assertThat(result).isEqualTo(ArticleCollectStepResultDto.empty(ArticleSource.HANKYUNG));
    then(stagingRepository).should(never()).saveAll(any());
  }

  @Test
  @DisplayName("성공 - collector 실패 결과를 수집 결과에 그대로 집계한다")
  void collectRssArticlesToStaging_success_keepCollectorFailureCounts() {
    // given
    Interest interest = interest("경제");
    given(interestKeywordRepository.findAll()).willReturn(List.of(interestKeyword(interest, "금리")));
    given(feedCollector.getSource()).willReturn(ArticleSource.HANKYUNG);
    given(feedCollector.collectLatestResult(100))
        .willReturn(new RssCollectResultDto(List.of(), 1, 0, 1));

    // when
    ArticleCollectStepResultDto result =
        articleCollectService.collectRssArticlesToStaging(JOB_INSTANCE_ID, ArticleSource.HANKYUNG);

    // then
    assertThat(result.source()).isEqualTo(ArticleSource.HANKYUNG);
    assertThat(result.requestCount()).isEqualTo(1);
    assertThat(result.successCount()).isZero();
    assertThat(result.failureCount()).isEqualTo(1);
    assertThat(result.stagedCount()).isZero();
  }

  @Test
  @DisplayName("성공 - sourceUrl이 null이거나 blank인 기사는 articles 저장 시 invalid로 스킵한다")
  void saveStagedArticlesAndLinkInterests_success_skipInvalidSourceUrl() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging("https://news.example.com/java", "Java 기사", "요약", ArticleSource.NAVER),
        staging(null, "Java URL 없음", "요약", ArticleSource.NAVER),
        staging(" ", "Java URL 공백", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(List.of());
    given(articleRepository.save(any(Article.class))).willAnswer(invocation -> articleWithId(
        invocation.getArgument(0)));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(false);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.stagedCount()).isEqualTo(3);
    assertThat(result.savedCount()).isEqualTo(1);
    assertThat(result.invalidSkippedCount()).isEqualTo(2);
    then(articleRepository).should(times(1)).save(any(Article.class));
  }

  @Test
  @DisplayName("성공 - 같은 sourceUrl 기사는 articles 저장 시 중복 제거한다")
  void saveStagedArticlesAndLinkInterests_success_deduplicateSameSourceUrlWhenSavingArticles() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging("https://news.example.com/java", "Java 첫 번째 기사", "요약", ArticleSource.NAVER),
        staging("https://news.example.com/java", "Java 두 번째 기사", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(List.of());
    given(articleRepository.save(any(Article.class))).willAnswer(invocation -> articleWithId(
        invocation.getArgument(0)));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(false);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.stagedCount()).isEqualTo(2);
    assertThat(result.savedCount()).isEqualTo(1);
    assertThat(result.duplicateSkippedCount()).isEqualTo(1);
    then(articleRepository).should(times(1)).save(any(Article.class));
  }

  @Test
  @DisplayName("성공 - staging 기사 저장 후 ArticleInterest를 연결하고 관심사 매칭 이벤트를 발행한다")
  void saveStagedArticlesAndLinkInterests_success_linkArticleInterestAndPublishEvent() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging("https://news.example.com/java", "Java 기사", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(List.of());
    given(articleRepository.save(any(Article.class))).willAnswer(invocation -> articleWithId(
        invocation.getArgument(0)));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(false);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.articleInterestLinkedCount()).isEqualTo(1);
    then(articleInterestRepository).should().save(any(ArticleInterest.class));

    ArgumentCaptor<InterestMatchedArticleEvent> eventCaptor =
        ArgumentCaptor.forClass(InterestMatchedArticleEvent.class);
    then(batchEventPublisher).should().publish(eventCaptor.capture());
    assertThat(eventCaptor.getValue().interests())
        .extracting(InterestMatchedArticleEvent.InterestMatchData::interestId)
        .containsExactly(interest.getId());
  }

  @Test
  @DisplayName("성공 - 이미 DB에 존재하는 article은 새로 저장하지 않고 관심사만 연결한다")
  void saveStagedArticlesAndLinkInterests_success_linkInterestWithoutSavingExistingArticle() {
    // given
    Interest interest = interest("개발");
    Article existingArticle = article("https://news.example.com/java", "Java 기사", "요약");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging(existingArticle.getSourceUrl(), "Java 기사", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(
        List.of(existingArticle));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(false);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.savedCount()).isZero();
    assertThat(result.duplicateSkippedCount()).isEqualTo(1);
    assertThat(result.articleInterestLinkedCount()).isEqualTo(1);
    then(articleRepository).should(never()).save(any(Article.class));
    then(articleInterestRepository).should().save(any(ArticleInterest.class));
  }

  @Test
  @DisplayName("성공 - 이미 존재하는 ArticleInterest는 중복 저장하지 않는다")
  void saveStagedArticlesAndLinkInterests_success_skipExistingArticleInterest() {
    // given
    Interest interest = interest("개발");
    Article existingArticle = article("https://news.example.com/java", "Java 기사", "요약");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "java")));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging(existingArticle.getSourceUrl(), "Java 기사", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(
        List.of(existingArticle));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(true);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.articleInterestLinkedCount()).isZero();
    then(articleInterestRepository).should(never()).save(any(ArticleInterest.class));
    then(batchEventPublisher).should(never()).publish(any());
  }

  @Test
  @DisplayName("성공 - 하나의 관심사에 여러 키워드가 매칭돼도 ArticleInterest는 한 번만 연결한다")
  void saveStagedArticlesAndLinkInterests_success_linkOnceWhenMultipleKeywordsMatchSameInterest() {
    // given
    Interest interest = interest("AI");
    given(interestKeywordRepository.findAll()).willReturn(List.of(
        interestKeyword(interest, "AI"),
        interestKeyword(interest, "인공지능")
    ));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging("https://news.example.com/ai", "AI와 인공지능 기사", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(List.of());
    given(articleRepository.save(any(Article.class))).willAnswer(invocation -> articleWithId(
        invocation.getArgument(0)));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(false);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.articleInterestLinkedCount()).isEqualTo(1);
    then(articleInterestRepository).should(times(1)).save(any(ArticleInterest.class));
  }

  @Test
  @DisplayName("성공 - 키워드의 대소문자와 앞뒤 공백을 정규화해서 매칭한다")
  void saveStagedArticlesAndLinkInterests_success_matchKeywordIgnoringCaseAndTrim() {
    // given
    Interest interest = interest("개발");
    given(interestKeywordRepository.findAll()).willReturn(
        List.of(interestKeyword(interest, "  Java  ")));
    given(stagingRepository.findAllByJobInstanceId(JOB_INSTANCE_ID)).willReturn(List.of(
        staging("https://news.example.com/java", "java news", "요약", ArticleSource.NAVER)
    ));
    given(articleRepository.findAllBySourceUrlIn(anyCollection())).willReturn(List.of());
    given(articleRepository.save(any(Article.class))).willAnswer(invocation -> articleWithId(
        invocation.getArgument(0)));
    given(articleInterestRepository.existsById(any(ArticleInterestId.class))).willReturn(false);

    // when
    ArticleSaveAndInterestLinkStepResultDto result =
        articleCollectService.saveStagedArticlesAndLinkInterests(JOB_INSTANCE_ID);

    // then
    assertThat(result.articleInterestLinkedCount()).isEqualTo(1);
    then(articleInterestRepository).should().save(any(ArticleInterest.class));
  }

  @Test
  @DisplayName("성공 - cleanupStaging은 JobInstanceId 기준으로 staging 데이터를 삭제한다")
  void cleanupStaging_success_deleteByJobInstanceId() {
    // given
    given(stagingRepository.deleteByJobInstanceId(JOB_INSTANCE_ID)).willReturn(3L);

    // when
    ArticleCollectStagingCleanupResultDto result =
        articleCollectService.cleanupStaging(JOB_INSTANCE_ID);

    // then
    assertThat(result.deletedCount()).isEqualTo(3L);
    then(stagingRepository).should().deleteByJobInstanceId(JOB_INSTANCE_ID);
  }

  private ArticleCollectService service(
      List<KeywordBasedArticleCollector> keywordCollectors,
      List<FeedBasedArticleCollector> feedCollectors
  ) {
    return new ArticleCollectService(
        keywordCollectors,
        feedCollectors,
        new ArticleCollectProperties(100, 100, 1, 1, 1, "0 0 * * * *"),
        interestKeywordRepository,
        articleRepository,
        articleInterestRepository,
        stagingRepository,
        batchEventPublisher
    );
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Iterable<ArticleCollectStaging>> stagingCaptor() {
    return ArgumentCaptor.forClass(Iterable.class);
  }

  private CollectedArticleDto collected(
      String sourceUrl,
      String title,
      String summary,
      ArticleSource source
  ) {
    return new CollectedArticleDto(source, sourceUrl, title, PUBLISH_DATE, summary);
  }

  private ArticleCollectStaging staging(
      String sourceUrl,
      String title,
      String summary,
      ArticleSource source
  ) {
    return new ArticleCollectStaging(JOB_INSTANCE_ID,
        collected(sourceUrl, title, summary, source));
  }

  private Interest interest(String name) {
    Interest interest = new Interest();
    ReflectionTestUtils.setField(interest, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(interest, "name", name);
    ReflectionTestUtils.setField(interest, "subscriberCount", 1L);
    ReflectionTestUtils.setField(interest, "createdAt", PUBLISH_DATE);
    ReflectionTestUtils.setField(interest, "updatedAt", PUBLISH_DATE);
    return interest;
  }

  private InterestKeyword interestKeyword(Interest interest, String keyword) {
    InterestKeyword interestKeyword = new InterestKeyword();
    ReflectionTestUtils.setField(interestKeyword, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(interestKeyword, "interest", interest);
    ReflectionTestUtils.setField(interestKeyword, "keyword", keyword);
    ReflectionTestUtils.setField(interestKeyword, "createdAt", PUBLISH_DATE);
    return interestKeyword;
  }

  private Article article(String sourceUrl, String title, String summary) {
    Article article = new Article(ArticleSource.NAVER, sourceUrl, title, PUBLISH_DATE, summary);
    ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    return article;
  }

  private Article articleWithId(Article article) {
    if (article.getId() == null) {
      ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
    }
    return article;
  }
}
