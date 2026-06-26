package com.monew.server.interest.entity;

import com.monew.server.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SubscriptionTest {

    @Test
    @DisplayName("구독을 생성하면 사용자와 관심사가 저장된다")
    void createSubscription() {
        // given
        User user = new User("woody@monew.com", "우디", "encodedPassword");
        Interest interest = new Interest("스포츠");

        // when
        Subscription subscription = new Subscription(user, interest);

        // then
        assertThat(subscription.getUser()).isSameAs(user);
        assertThat(subscription.getInterest()).isSameAs(interest);
    }
}
