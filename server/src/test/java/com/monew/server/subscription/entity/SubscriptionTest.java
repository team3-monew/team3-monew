package com.monew.server.subscription.entity;

import com.monew.server.interest.entity.Interest;
import com.monew.server.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;

public class SubscriptionTest {

    @Test
    @DisplayName("구독을 생성하면 사용자와 관심사가 설정된다")
    void createSubscription() {
        // given
        User user = mock(User.class);
        Interest interest = new Interest("스포츠");

        // when
        Subscription subscription = new Subscription(user, interest);

        // then
        assertThat(subscription.getUser()).isEqualTo(user);
        assertThat(subscription.getInterest()).isEqualTo(interest);
    }
}
