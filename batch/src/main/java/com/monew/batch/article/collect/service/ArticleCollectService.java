package com.monew.batch.article.collect.service;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기사 수집 배치의 실제 업무 로직을 담당하는 서비스입니다.
 * 각 수집 Step은 외부 API/RSS 호출 결과를 바로 articles에 저장하지 않고 staging 테이블에 먼저 저장하며,
 * 이후 저장 Step과 관심사 연결 Step이 staging 데이터를 기준으로 후속 처리를 수행합니다.
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
  private final ArticleCollectStagingRepository stagingRepository;

  // Naver 뉴스 검색 API로 기사 수집
  @Transactional
  public ArticleCollectStepResultDto collectNaverArticlesToStaging(Long jobExecutionId) {
    // 관심사 키워드 찾기
    List<InterestKeyword> interestKeywords = findInterestKeywords();
    if (interestKeywords.isEmpty()) {
      log.info("Skip Naver article collect because no interest keywords exist.");
      return ArticleCollectStepResultDto.empty(ArticleSource.NAVER);
    }

    Set<String> keywords = groupByNormalizedKeyword(interestKeywords).keySet();
    List<CollectedArticleDto> collectedArticles = new ArrayList<>();
    int requestCount = 0;
    int successCount = 0;
    int failureCount = 0;

    // naver api 호출해서 기사 수집
    for (KeywordBasedArticleCollector collector : keywordCollectors) {
      for (String keyword : keywords) {
        KeywordCollectResultDto collected = collector.collectByKeywordResult(keyword,
            properties.naverDisplay());
        collectedArticles.addAll(collected.articles());
        requestCount += collected.requestCount();
        successCount += collected.successCount();
        failureCount += collected.failureCount();
      }
    }

    // 관심사 포함된 기사 필터링
    List<CollectedArticleDto> matchedArticles = filterMatchedArticles(collectedArticles,
        interestKeywords);
    // db에 staging 기사들 저장
    StageResult stageResult = saveStagingArticles(jobExecutionId, matchedArticles);

    ArticleCollectStepResultDto result = new ArticleCollectStepResultDto(
        ArticleSource.NAVER,
        requestCount,
        successCount,
        failureCount,
        collectedArticles.size(),
        matchedArticles.size(),
        stageResult.savedCount(),
        stageResult.duplicateSkippedCount()
    );
    log.info("Naver collect step finished. result={}", result);
    return result;
  }

  // RSS로 뉴스 기사 수집
  @Transactional
  public ArticleCollectStepResultDto collectRssArticlesToStaging(Long jobExecutionId,
      ArticleSource source) {
    // 관심사 키워드 찾기
    List<InterestKeyword> interestKeywords = findInterestKeywords();
    if (interestKeywords.isEmpty()) {
      log.info("Skip RSS article collect because no interest keywords exist. source={}", source);
      return ArticleCollectStepResultDto.empty(source);
    }

    // 뉴스 기사 출처에 해당하는 수집기 찾기
    FeedBasedArticleCollector collector = findFeedCollector(source);
    if (collector == null) {
      log.warn("Skip RSS article collect because collector does not exist. source={}", source);
      return ArticleCollectStepResultDto.empty(source);
    }

    // 수집기 통해 뉴스 기사 수집
    RssCollectResultDto collectResult = collector.collectLatestResult(properties.rssLimit());
    // 관심사 포함된 기사 필터링
    List<CollectedArticleDto> matchedArticles = filterMatchedArticles(collectResult.articles(),
        interestKeywords);
    // db에 staging 기사들 저장
    StageResult stageResult = saveStagingArticles(jobExecutionId, matchedArticles);

    ArticleCollectStepResultDto result = new ArticleCollectStepResultDto(
        source,
        collectResult.requestCount(),
        collectResult.successCount(),
        collectResult.failureCount(),
        collectResult.articles().size(),
        matchedArticles.size(),
        stageResult.savedCount(),
        stageResult.duplicateSkippedCount()
    );
    log.info("RSS collect step finished. result={}", result);
    return result;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleSaveAndInterestLinkStepResultDto saveStagedArticlesAndLinkInterests(
      Long jobExecutionId) {
    List<InterestKeyword> interestKeywords = findInterestKeywords();
    List<CollectedArticleDto> stagedArticles = stagingRepository.findAllByJobExecutionId(
            jobExecutionId)
        .stream()
        .map(ArticleCollectStaging::toCollectedArticleDto)
        .toList();

    SaveResult saveResult = saveArticles(stagedArticles);
    int linkedCount = saveArticleInterests(saveResult.articlesByUrl().values(), interestKeywords);

    ArticleSaveAndInterestLinkStepResultDto result = new ArticleSaveAndInterestLinkStepResultDto(
        stagedArticles.size(),
        saveResult.savedCount(),
        saveResult.duplicateSkippedCount(),
        saveResult.invalidSkippedCount(),
        linkedCount
    );
    log.info("Article save and interest link step finished. result={}", result);
    return result;
  }

  /**
   * 이번 배치 실행에서 사용한 staging 데이터를 삭제합니다.
   * 저장과 관심사 연결이 끝난 뒤 호출되므로 staging 테이블은 중간 저장소 역할만 하고 비워집니다.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleCollectStagingCleanupResultDto cleanupStaging(Long jobExecutionId) {
    long deletedCount = stagingRepository.deleteByJobExecutionId(jobExecutionId);
    ArticleCollectStagingCleanupResultDto result =
        new ArticleCollectStagingCleanupResultDto(deletedCount);
    log.info("Article collect staging cleanup step finished. result={}", result);
    return result;
  }

  private List<InterestKeyword> findInterestKeywords() {
    return interestKeywordRepository.findAll();
  }

  /**
   * RSS 수집기 목록에서 요청한 출처를 담당하는 수집기를 찾습니다.
   */
  private FeedBasedArticleCollector findFeedCollector(ArticleSource source) {
    return feedCollectors.stream()
        .filter(collector -> collector.getSource() == source)
        .findFirst()
        .orElse(null);
  }

  /**
   * 제목과 요약에 관심사 키워드가 하나라도 포함된 기사만 저장 후보로 남깁니다.
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
   * 수집 Step에서 얻은 저장 후보 기사를 staging 테이블에 저장합니다.
   * 같은 배치 실행 안에서 sourceUrl이 중복되면 첫 번째 기사만 남기고 나머지는 건너뜁니다.
   */
  private StageResult saveStagingArticles(Long jobExecutionId,
      List<CollectedArticleDto> collectedArticleDtos) {
    int invalidSkippedCount = (int) collectedArticleDtos.stream()
        .filter(article -> article.sourceUrl() == null || article.sourceUrl().isBlank())
        .count();

    // sourceUrl 중복 시 첫번째 기사만 남김
    Map<String, CollectedArticleDto> collectedByUrl = collectedArticleDtos.stream()
        .filter(article -> article.sourceUrl() != null && !article.sourceUrl().isBlank())
        .collect(Collectors.toMap(
            CollectedArticleDto::sourceUrl,
            Function.identity(),
            (first, ignored) -> first,
            LinkedHashMap::new
        ));

    // 현재 실행중인 job에서 staging 테이블에 저장되어 있는 url 목록
    Set<String> existingStagingUrls = stagingRepository.findAllByJobExecutionIdAndSourceUrlIn(
            jobExecutionId, collectedByUrl.keySet())
        .stream()
        .map(ArticleCollectStaging::getSourceUrl)
        .collect(Collectors.toSet());

    // staging에 없는 기사들 목록
    List<ArticleCollectStaging> stagingArticles = collectedByUrl.entrySet().stream()
        .filter(entry -> !existingStagingUrls.contains(entry.getKey()))
        .map(entry -> new ArticleCollectStaging(jobExecutionId, entry.getValue()))
        .toList();
    // staging에 새 기사들 저장
    stagingRepository.saveAll(stagingArticles);

    int duplicateSkippedCount =
        collectedArticleDtos.size() - invalidSkippedCount - collectedByUrl.size()
            + existingStagingUrls.size();
    return new StageResult(stagingArticles.size(), duplicateSkippedCount, invalidSkippedCount);
  }

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
        articlesByUrl.put(entry.getKey(), articleRepository.save(Article.from(entry.getValue())));
        savedCount++;
      }
    }

    int duplicateSkippedCount =
        collectedArticleDtos.size() - invalidSkippedCount - collectedByUrl.size()
            + existingCount;
    return new SaveResult(articlesByUrl, savedCount, duplicateSkippedCount, invalidSkippedCount);
  }

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

  /**
   * 기사 제목/요약에 포함된 키워드를 기준으로 매칭되는 관심사 목록을 찾습니다.
   * 하나의 관심사에 여러 키워드가 매칭되어도 같은 관심사는 한 번만 반환합니다.
   */
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
   * 같은 키워드가 여러 관심사에 등록되어 있어도 외부 API 요청은 한 번만 보내도록 정규화한 키워드로 묶습니다.
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

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.toLowerCase(Locale.ROOT).trim();
  }

  private record StageResult(
      int savedCount,
      int duplicateSkippedCount,
      int invalidSkippedCount
  ) {
  }

  private record SaveResult(
      Map<String, Article> articlesByUrl,
      int savedCount,
      int duplicateSkippedCount,
      int invalidSkippedCount
  ) {
  }
}
