package com.monew.server.interest.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class InterestTest {

    @Test
    @DisplayName("관심사를 생성하면 이름이 저장되고 구독자 수는 0으로 초기화된다")
    void createInterest() {
        // given
        String name = "스포츠";

        // when
        Interest interest = new Interest(name);

        // then
        assertThat(interest.getName()).isEqualTo(name);
        assertThat(interest.getSubscriberCount()).isZero();
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
    @DisplayName("구독자 수는 0 미만으로 감소하지 않는다")
    void decreaseSubscriberCountDoesNotGoBelowZero() {
        // given
        Interest interest = new Interest("스포츠");

        // when
        interest.decreaseSubscriberCount();

        // then
        assertThat(interest.getSubscriberCount()).isZero();
    }

    @Test
    @DisplayName("수정 시각을 갱신할 수 있다")
    void markUpdated() {
        // given
        Interest interest = new Interest("스포츠");
        LocalDateTime before = LocalDateTime.now();

        // when
        interest.markUpdated();

        // then
        assertThat(interest.getUpdatedAt()).isNotNull();
        assertThat(interest.getUpdatedAt()).isAfterOrEqualTo(before);
    }
}
