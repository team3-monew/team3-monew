package com.monew.server.interest.repository;

import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InterestRepositoryImplTest extends RepositoryTestSupport {

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestKeywordRepository interestKeywordRepository;

    @Test
    @DisplayName("관심사 이름에 검색어가 포함되면 관심사를 조회한다")
    void searchInterests_byInterestName() {
        // given
        saveInterest("sports", List.of("football"), 0);
        saveInterest("economy", List.of("stock"), 0);

        // when
        List<Interest> result = interestRepository.searchInterests("sport", "name", "ASC",
                null, null, 10);

        // then
        assertThat(result)
                .extracting(Interest::getName)
                .containsExactly("sports");
    }

    @Test
    @DisplayName("관심사 키워드에 검색어가 포함되면 관심사를 조회한다")
    void searchInterests_byKeyword() {
        // given
        saveInterest("sports", List.of("football", "baseball"), 0);
        saveInterest("economy", List.of("stock"), 0);

        // when
        List<Interest> result = interestRepository.searchInterests("base", "name", "ASC",
                null, null, 10);

        // then
        assertThat(result)
                .extracting(Interest::getName)
                .containsExactly("sports");
    }

    @Test
    @DisplayName("구독자 수 기준으로 내림차순 정렬할 수 있다")
    void searchInterests_orderBySubscriberCountDesc() {
        // given
        saveInterest("alpha", List.of("a"), 1);
        saveInterest("beta", List.of("b"), 3);
        saveInterest("gamma", List.of("c"), 2);

        // when
        List<Interest> result = interestRepository.searchInterests(null, "subscriberCount",
                "DESC", null, null, 10);

        // then
        assertThat(result)
                .extracting(Interest::getName)
                .containsExactly("beta", "gamma", "alpha");
    }

    @Test
    @DisplayName("관심사 이름 기준 커서 조건으로 다음 페이지를 조회할 수 있다")
    void searchInterests_withNameCursor() {
        // given
        Interest alpha = saveInterest("alpha", List.of("a"), 0);
        saveInterest("beta", List.of("b"), 0);
        saveInterest("gamma", List.of("c"), 0);

        String cursor = alpha.getName() + "|" + alpha.getId();
        LocalDateTime after = alpha.getCreatedAt();

        // when
        List<Interest> result = interestRepository.searchInterests(null, "name", "ASC", cursor,
                after, 10);

        // then
        assertThat(result)
                .extracting(Interest::getName)
                .containsExactly("beta", "gamma");
    }

    @Test
    @DisplayName("검색 조건에 맞는 관심사 개수를 조회한다")
    void countInterests_withKeyword() {
        // given
        saveInterest("sports", List.of("football", "baseball"), 0);
        saveInterest("economy", List.of("stock"), 0);
        saveInterest("tech", List.of("ai"), 0);

        // when
        long count = interestRepository.countInterests("ball");

        // then
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("pg_trgm 기반으로 유사 관심사 후보를 조회한다")
    void findSimilarNameCandidates_byPgTrgm() {
        // given
        saveInterest("인공 지능", List.of("AI"), 0);
        saveInterest("축구", List.of("스포츠"), 0);

        // when
        List<String> result = interestRepository.findSimilarNameCandidates("인공지능");

        // then
        assertThat(result).contains("인공 지능");
        assertThat(result).doesNotContain("축구");
    }

    private Interest saveInterest(String name, List<String> keywords, int subscriberCount) {
        Interest interest = new Interest(name);

        for (int i = 0; i < subscriberCount; i++) {
            interest.increaseSubscriberCount();
        }

        Interest savedInterest = interestRepository.saveAndFlush(interest);

        List<InterestKeyword> interestKeywords = keywords.stream()
                .map(keyword -> new InterestKeyword(savedInterest, keyword))
                .toList();

        interestKeywordRepository.saveAllAndFlush(interestKeywords);

        return savedInterest;
    }
}
