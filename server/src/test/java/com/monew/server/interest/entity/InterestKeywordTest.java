package com.monew.server.interest.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InterestKeywordTest {

    @Test
    @DisplayName("관심사 키워드를 생성하면 관심사와 키워드가 저장된다")
    void createInterestKeyword() {
        // given
        Interest interest = new Interest("스포츠");
        String keyword = "축구";

        // when
        InterestKeyword interestKeyword = new InterestKeyword(interest, keyword);

        // then
        assertThat(interestKeyword.getInterest()).isSameAs(interest);
        assertThat(interestKeyword.getKeyword()).isEqualTo(keyword);
    }
}
