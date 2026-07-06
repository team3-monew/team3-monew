package com.monew.server.interest.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InterestTest {

    @Test
    @DisplayName("관심사를 생성하면 이름, 정규화 이름, 초기 구독자 수, 수정 시간이 설정된다")
    void createInterest() {
        // given
        String name = "인공 지능";

        // when
        Interest interest = new Interest(name);

        // then
        assertThat(interest.getName()).isEqualTo(name);
        assertThat(interest.getNormalizedName()).isEqualTo("인공지능");
        assertThat(interest.getSubscriberCount()).isZero();
        assertThat(interest.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("관심사 이름은 소문자 변환과 공백 제거 방식으로 정규화된다")
    void normalizedName() {
        // expect
        assertThat(Interest.normalizeName(" AI 뉴스 ")).isEqualTo("ai뉴스");
        assertThat(Interest.normalizeName("인공 지능")).isEqualTo("인공지능");
        assertThat(Interest.normalizeName("  스포츠  ")).isEqualTo("스포츠");
    }

    @Test
    @DisplayName("null 이름을 정규화하면 빈 문자열을 반환한다")
    void normalizedNullName() {
        // expect
        assertThat(Interest.normalizeName(null)).isEmpty();
    }

    @Test
    @DisplayName("구독자 수를 증가시킬 수 있다")
    void increaseSubscriberCount() {
        // given
        Interest interest = new Interest("스포츠");

        // when
        interest.increaseSubscriberCount();

        // then
        assertThat(interest.getSubscriberCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("구독자 수를 감소시킬 수 있다")
    void decreaseSubscriberCount() {
        // given
        Interest interest = new Interest("스포츠");
        interest.increaseSubscriberCount();

        // when
        interest.decreaseSubscriberCount();

        // then
        assertThat(interest.getSubscriberCount()).isZero();
    }

    @Test
    @DisplayName("구독자 수는 0보다 작아지지 않는다")
    void decreaseSubscriberCountDoesNotGoBelowZero() {
        // given
        Interest interest = new Interest("스포츠");

        // when
        interest.decreaseSubscriberCount();

        // then
        assertThat(interest.getSubscriberCount()).isZero();
    }

    @Test
    @DisplayName("수정 시간을 갱신할 수 있다")
    void markUpdated() {
        // given
        Interest interest = new Interest("스포츠");
        var beforeUpdatedAt = interest.getUpdatedAt();

        // when
        interest.markUpdated();

        // then
        assertThat(interest.getUpdatedAt()).isNotNull();
        assertThat(interest.getUpdatedAt()).isAfterOrEqualTo(beforeUpdatedAt);
    }
}
