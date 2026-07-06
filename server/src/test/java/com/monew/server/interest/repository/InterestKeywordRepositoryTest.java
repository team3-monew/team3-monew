package com.monew.server.interest.repository;

import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import com.monew.server.support.RepositoryTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterestKeywordRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private InterestKeywordRepository interestKeywordRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기존 키워드를 포함한 최종 키워드 목록으로 수정해도 중복 제약 오류가 발생하지 않는다")
    void updateKeywords_withExistingKeywords_doesNotViolateUniqueConstraint() {
        // given
        Interest interest = interestRepository.saveAndFlush(new Interest("개발"));

        interestKeywordRepository.saveAllAndFlush(List.of(
                new InterestKeyword(interest, "컴퓨터"),
                new InterestKeyword(interest, "안녕")
        ));

        entityManager.clear();

        // when
        interestKeywordRepository.deleteByInterestId(interest.getId());

        Interest managedInterest = interestRepository.findById(interest.getId())
                .orElseThrow();

        interestKeywordRepository.saveAllAndFlush(List.of(
                new InterestKeyword(managedInterest, "컴퓨터"),
                new InterestKeyword(managedInterest, "안녕"),
                new InterestKeyword(managedInterest, "바이")
        ));

        // then
        List<String> keywords = interestKeywordRepository.findAllByInterestId(interest.getId()).stream()
                .map(InterestKeyword::getKeyword)
                .toList();

        assertThat(keywords)
                .containsExactlyInAnyOrder("컴퓨터", "안녕", "바이");
    }
}