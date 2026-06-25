package com.monew.batch.article.collect.service;

import com.monew.batch.article.collect.collector.CollectedArticle;
import com.monew.batch.article.collect.collector.KeywordBasedArticleCollector;
import com.monew.batch.article.config.ArticleCollectProperties;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleCollectService {

  private final List<KeywordBasedArticleCollector> collectors;
  private final ArticleCollectProperties properties;
  private final InterestKeywordRepository interestKeywordRepository;
  private final ArticleRepository articleRepository;
  private final ArticleInterestRepository articleInterestRepository;

  /**
   * 기사 수집 배치의 전체 흐름입니다.
   * 1. 관심사 키워드를 조회하고, 2. 키워드별 기사를 수집하고,
   * 3. URL 기준으로 Article을 저장한 뒤, 4. 제목/요약에 맞는 관심사와 매핑합니다.
   */
  @Transactional
  public void collect() {
    List<InterestKeyword> interestKeywords = interestKeywordRepository.findAll();
    if (interestKeywords.isEmpty()) {
      log.info("관심 키워드 존재 안 함. 기사 수집 skip");
      return;
    }

    Map<String, List<InterestKeyword>> interestKeywordsByKeyword = groupByNormalizedKeyword(interestKeywords);
    List<CollectedArticle> collectedArticles = collectArticles(interestKeywordsByKeyword.keySet());
    Map<String, Article> articlesByUrl = saveArticles(collectedArticles);
    int savedMappings = saveArticleInterests(articlesByUrl.values(), interestKeywords);

    log.info("Article collect finished. collected={}, articles={}, articleInterests={}",
        collectedArticles.size(), articlesByUrl.size(), savedMappings);
  }


  //키워드에 해당되는 기사 가져오는 메서드
  private List<CollectedArticle> collectArticles(Set<String> keywords) {
    List<CollectedArticle> articles = new ArrayList<>();

    for (KeywordBasedArticleCollector collector : collectors) {
      for (String keyword : keywords) {
        articles.addAll(collector.collectByKeyword(keyword, properties.naverDisplay()));
      }
    }

    return articles;
  }

  // 중복 기사 제거 + db에 article 저장
  private Map<String, Article> saveArticles(List<CollectedArticle> collectedArticles) {
    // key 값 : sourceUrl
    Map<String, CollectedArticle> collectedByUrl = collectedArticles.stream()
        .filter(article -> article.sourceUrl() != null && !article.sourceUrl().isBlank())
        .collect(Collectors.toMap(
            CollectedArticle::sourceUrl,
            Function.identity(),
            (first, ignored) -> first,
            LinkedHashMap::new
        ));

    Map<String, Article> articlesByUrl = articleRepository.findAllBySourceUrlIn(collectedByUrl.keySet())
        .stream()
        .collect(Collectors.toMap(Article::getSourceUrl, Function.identity()));

    for (Map.Entry<String, CollectedArticle> entry : collectedByUrl.entrySet()) {
      articlesByUrl.computeIfAbsent(entry.getKey(), ignored -> articleRepository.save(toArticle(entry.getValue())));
    }

    return articlesByUrl;
  }

  private int saveArticleInterests(Iterable<Article> articles, List<InterestKeyword> interestKeywords) {
    int savedCount = 0;
    Set<ArticleInterestId> seenIds = new LinkedHashSet<>();

    for (Article article : articles) {
      for (Interest interest : matchInterests(article, interestKeywords)) {
        ArticleInterestId id = new ArticleInterestId(article.getId(), interest.getId());
        if (seenIds.add(id) && !articleInterestRepository.existsById(id)) {
          articleInterestRepository.save(new ArticleInterest(article, interest));
          savedCount++;
        }
      }
    }
    return savedCount;
  }

  // 기사 제목과 요약에 어떤 관심사 키워드 포함되는지 확인
  private List<Interest> matchInterests(Article article, List<InterestKeyword> interestKeywords) {
    String searchableText = normalize(article.getTitle() + " " + article.getSummary());
    Map<UUID, Interest> matchedInterests = new LinkedHashMap<>();

    for (InterestKeyword interestKeyword : interestKeywords) {
      String keyword = normalize(interestKeyword.getKeyword());
      if (!keyword.isBlank() && searchableText.contains(keyword)) {
        matchedInterests.putIfAbsent(interestKeyword.getInterest().getId(), interestKeyword.getInterest());
      }
    }
    return List.copyOf(matchedInterests.values());
  }

  // 같은 키워드가 여러 관심사에 등록되어 있어도 API 호출 한 번만 하도록 키워드 정규화해 묶기
  private Map<String, List<InterestKeyword>> groupByNormalizedKeyword(List<InterestKeyword> interestKeywords) {
    return interestKeywords.stream()
        .filter(interestKeyword -> interestKeyword.getKeyword() != null
            && !interestKeyword.getKeyword().isBlank())
        .collect(Collectors.groupingBy(
            interestKeyword -> normalize(interestKeyword.getKeyword()),
            LinkedHashMap::new,
            Collectors.toList()
        ));
  }

  // 키워드 소문자 변환 + 앞 뒤 공백 제거 수행
  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.toLowerCase(Locale.ROOT).trim();
  }

  private Article toArticle(CollectedArticle collectedArticle) {
    return new Article(
        collectedArticle.source(),
        collectedArticle.sourceUrl(),
        collectedArticle.title(),
        collectedArticle.publishDate(),
        collectedArticle.summary()
    );
  }
}
