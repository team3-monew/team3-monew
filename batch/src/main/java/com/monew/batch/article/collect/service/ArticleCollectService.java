package com.monew.batch.article.collect.service;

import com.monew.batch.article.collect.collector.dto.CollectedArticleDto;
import com.monew.batch.article.collect.collector.FeedBasedArticleCollector;
import com.monew.batch.article.collect.collector.KeywordBasedArticleCollector;
import com.monew.batch.article.collect.collector.dto.RssCollectResultDto;
import com.monew.batch.article.collect.dto.ArticleCollectResultDto;
import com.monew.batch.article.config.ArticleCollectProperties;
import com.monew.batch.article.entity.ArticleSource;
import com.monew.batch.article.entity.Article;
import com.monew.batch.article.entity.ArticleInterest;
import com.monew.batch.article.entity.ArticleInterestId;
import com.monew.batch.article.repository.ArticleInterestRepository;
import com.monew.batch.article.repository.ArticleRepository;
import com.monew.batch.interest.entity.Interest;
import com.monew.batch.interest.entity.InterestKeyword;
import com.monew.batch.interest.repository.InterestKeywordRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기사 수집 배치의 전체 흐름을 조율하는 서비스입니다. Naver 키워드 검색을 먼저 실행하고, 그 다음 RSS 수집 결과를 합쳐 기존 저장/관심사 연결 로직으로 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectService {

  private final List<KeywordBasedArticleCollector> keywordCollectors;
  private final List<FeedBasedArticleCollector> feedCollectors;
  private final ArticleCollectProperties properties;
  private final InterestKeywordRepository interestKeywordRepository;
  private final ArticleRepository articleRepository;
  private final ArticleInterestRepository articleInterestRepository;

  @Transactional
  public ArticleCollectResultDto collect() {
    // 관심사 키워드 조회
    List<InterestKeyword> interestKeywords = interestKeywordRepository.findAll();
    if (interestKeywords.isEmpty()) {
      log.info("Skip article collect because no interest keywords exist.");
      return emptyResult();
    }

    Map<String, List<InterestKeyword>> interestKeywordsByKeyword = groupByNormalizedKeyword(
        interestKeywords);
    // 관심사 기반 뉴스 api 호출해 기사 수집
    CollectArticleStepResult keywordResult = collectKeywordArticles(interestKeywordsByKeyword.keySet());
    // RSS 기사 수집
    CollectArticleStepResult feedResult = collectFeedArticles();

    List<CollectedArticleDto> collectedArticleDtos = new ArrayList<>();
    collectedArticleDtos.addAll(keywordResult.articles());
    collectedArticleDtos.addAll(feedResult.articles());

    // 키워드 매칭
    List<CollectedArticleDto> matchedArticles = filterMatchedArticles(collectedArticleDtos,
        interestKeywords);
    // 중복 체크 후 db에 기사 저장
    SaveResult saveResult = saveArticles(matchedArticles);
    // ArticleInterest 저장
    int savedMappings = saveArticleInterests(saveResult.articlesByUrl().values(), interestKeywords);

    ArticleCollectResultDto result = new ArticleCollectResultDto(
        keywordResult.requestCount(),
        keywordResult.successCount(),
        keywordResult.failureCount(),
        false,
        feedResult.requestCount(),
        feedResult.successCount(),
        feedResult.failureCount(),
        feedResult.yeonhapRequestCount(),
        feedResult.yeonhapSuccessCount(),
        feedResult.yeonhapFailureCount(),
        collectedArticleDtos.size(),
        saveResult.savedCount(),
        saveResult.duplicateSkippedCount(),
        saveResult.invalidSkippedCount(),
        savedMappings,
        keywordResult.failureCount() + feedResult.failureCount()
    );

    log.info("Article collect finished. result={}", result);
    return result;
  }

  /**
   * 관심사 키워드를 기준으로 Naver 같은 키워드 기반 수집기를 실행합니다.
   * 수집기 내부에서 실패를 빈 목록으로 처리하므로, 일부 실패가 나도 다음 RSS 단계는 계속
   * 실행됩니다.
   */
  private CollectArticleStepResult collectKeywordArticles(Set<String> keywords) {
    List<CollectedArticleDto> articles = new ArrayList<>();
    int requestCount = 0;
    int successCount = 0;

    for (KeywordBasedArticleCollector collector : keywordCollectors) {
      for (String keyword : keywords) {
        requestCount++;
        List<CollectedArticleDto> collected = collector.collectByKeyword(keyword,
            properties.naverDisplay());
        articles.addAll(collected);
        successCount++;
      }
    }

    return new CollectArticleStepResult(articles, requestCount, successCount, requestCount - successCount,
        0, 0, 0);
  }

  /**
   * RSS 수집기들을 Order 순서대로 실행합니다.
   * 현재 순서는 한국경제 -> 조선일보 -> 연합뉴스TV이고, 연합뉴스TV는 내부에서 카테고리 RSS들을 다시 순회합니다.
   */
  private CollectArticleStepResult collectFeedArticles() {
    log.info("RSS 기사 수집 시작. collectorCount={}", feedCollectors.size());

    List<CollectedArticleDto> articles = new ArrayList<>();
    int requestCount = 0;
    int successCount = 0;
    int failureCount = 0;
    int yeonhapRequestCount = 0;
    int yeonhapSuccessCount = 0;
    int yeonhapFailureCount = 0;

    for (FeedBasedArticleCollector collector : feedCollectors) {
      // api 링크 없는 경우 null값 empty값 반환 해당 부분 어떻게 흘러가는지 체크 필요
      RssCollectResultDto result = collector.collectLatestResult(properties.rssLimit());
      requestCount += result.requestCount();
      successCount += result.successCount();
      failureCount += result.failureCount();
      articles.addAll(result.articles());

      if (collector.getSource() == ArticleSource.YEONHAP) {
        yeonhapRequestCount += result.requestCount();
        yeonhapSuccessCount += result.successCount();
        yeonhapFailureCount += result.failureCount();
      }
      log.info("RSS article collect finished for source={}. collected={}",
          collector.getSource(), result.articles().size());
    }

    log.info("Finish RSS article collect. collected={}", articles.size());
    return new CollectArticleStepResult(articles, requestCount, successCount, failureCount,
        yeonhapRequestCount, yeonhapSuccessCount, yeonhapFailureCount);
  }

  /**
   * 제목과 요약에 관심사 키워드가 하나도 포함되지 않은 기사는 저장 대상에서 제외합니다.
   */
  private List<CollectedArticleDto> filterMatchedArticles(
      List<CollectedArticleDto> collectedArticleDtos,
      List<InterestKeyword> interestKeywords) {
    return collectedArticleDtos.stream()
        .filter(article -> !matchInterests(article.title(), article.summary(),
            interestKeywords).isEmpty())
        .toList();
  }

  /**
   * DB에 없는 기사만 새 Article로 저장
   */
  private SaveResult saveArticles(List<CollectedArticleDto> collectedArticleDtos) {
    int invalidSkippedCount = (int) collectedArticleDtos.stream()
        .filter(article -> article.sourceUrl() == null || article.sourceUrl().isBlank())
        .count();

    Map<String, CollectedArticleDto> collectedByUrl = collectedArticleDtos.stream()
        .filter(article -> article.sourceUrl() != null && !article.sourceUrl().isBlank())
        .collect(Collectors.toMap(
            CollectedArticleDto::sourceUrl,
            Function.identity(),
            (first, ignored) -> first,
            LinkedHashMap::new
        ));

    Map<String, Article> articlesByUrl = articleRepository.findAllBySourceUrlIn(
            collectedByUrl.keySet())
        .stream()
        .collect(Collectors.toMap(Article::getSourceUrl, Function.identity()));

    int existingCount = articlesByUrl.size();
    int savedCount = 0;
    for (Map.Entry<String, CollectedArticleDto> entry : collectedByUrl.entrySet()) {
      if (!articlesByUrl.containsKey(entry.getKey())) {
        articlesByUrl.put(entry.getKey(), articleRepository.save(toArticle(entry.getValue())));
        savedCount++;
      }
    }

    int duplicateSkippedCount =
        collectedArticleDtos.size() - invalidSkippedCount - collectedByUrl.size()
            + existingCount;
    return new SaveResult(articlesByUrl, savedCount, duplicateSkippedCount, invalidSkippedCount);
  }

  /**
   * 저장된 기사 또는 이미 존재하던 기사를 관심사와 연결합니다.
   */
  private int saveArticleInterests(Iterable<Article> articles,
      List<InterestKeyword> interestKeywords) {
    int savedCount = 0;
    Set<ArticleInterestId> seenIds = new LinkedHashSet<>();

    for (Article article : articles) {
      for (Interest interest : matchInterests(article.getTitle(), article.getSummary(),
          interestKeywords)) {
        ArticleInterestId id = new ArticleInterestId(article.getId(), interest.getId());
        if (seenIds.add(id) && !articleInterestRepository.existsById(id)) {
          articleInterestRepository.save(new ArticleInterest(article, interest));
          savedCount++;
        }
      }
    }
    return savedCount;
  }

  private List<Interest> matchInterests(String title, String summary,
      List<InterestKeyword> interestKeywords) {
    String searchableText = normalize(title + " " + summary);
    Map<UUID, Interest> matchedInterests = new LinkedHashMap<>();

    for (InterestKeyword interestKeyword : interestKeywords) {
      String keyword = normalize(interestKeyword.getKeyword());
      if (!keyword.isBlank() && searchableText.contains(keyword)) {
        matchedInterests.putIfAbsent(interestKeyword.getInterest().getId(),
            interestKeyword.getInterest());
      }
    }
    return List.copyOf(matchedInterests.values());
  }

  /**
   * 같은 키워드가 여러 관심사에 등록되어 있어도 Naver API 호출은 한 번만 하도록 정규화해 묶습니다.
   */
  private Map<String, List<InterestKeyword>> groupByNormalizedKeyword(
      List<InterestKeyword> interestKeywords) {

    return interestKeywords.stream()
        .filter(interestKeyword -> interestKeyword.getKeyword() != null
            && !interestKeyword.getKeyword().isBlank())
        .collect(Collectors.groupingBy(
            interestKeyword -> normalize(interestKeyword.getKeyword()),
            LinkedHashMap::new,
            Collectors.toList()
        ));
  }

  /**
   * 키워드 비교가 일관되도록 null 방어, 소문자 변환, 앞뒤 공백 제거를 수행합니다.
   */
  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.toLowerCase(Locale.ROOT).trim();
  }

  /**
   * 외부 수집 결과 DTO를 DB 저장용 Article Entity로 변환합니다.
   */
  private Article toArticle(CollectedArticleDto collectedArticleDto) {
    return new Article(
        collectedArticleDto.source(),
        collectedArticleDto.sourceUrl(),
        collectedArticleDto.title(),
        collectedArticleDto.publishDate(),
        collectedArticleDto.summary()
    );
  }

  /**
   * 관심사 키워드가 없어 배치를 건너뛸 때 반환하는 빈 결과입니다.
   */
  private ArticleCollectResultDto emptyResult() {
    return new ArticleCollectResultDto(0, 0, 0, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  /**
   * Naver 또는 RSS 수집 단계의 기사 목록과 요청 성공/실패 카운트를 함께 담는 내부 DTO입니다.
   */
  private record CollectArticleStepResult(
      List<CollectedArticleDto> articles,
      int requestCount,
      int successCount,
      int failureCount,
      int yeonhapRequestCount,
      int yeonhapSuccessCount,
      int yeonhapFailureCount
  ) {

  }

  /**
   * Article 저장 단계에서 필요한 저장 결과와 스킵 카운트를 묶어 반환하기 위한 내부 DTO입니다.
   */
  private record SaveResult(
      Map<String, Article> articlesByUrl,
      int savedCount,
      int duplicateSkippedCount,
      int invalidSkippedCount
  ) {

  }
}
