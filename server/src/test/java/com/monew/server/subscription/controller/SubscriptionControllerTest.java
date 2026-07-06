package com.monew.server.subscription.controller;

import com.monew.server.interest.entity.Interest;
import com.monew.server.subscription.dto.SubscriptionDto;
import com.monew.server.subscription.entity.Subscription;
import com.monew.server.subscription.service.SubscriptionService;
import com.monew.server.support.ControllerTestSupport;
import com.monew.server.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest extends ControllerTestSupport {

    private static final String AUTH_HEADER = "Monew-Request-User-ID";

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("관심사를 구독하면 200 OK와 구독 응답을 반환한다")
    void subscribe_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        User user = new User("test@example.com", "테스트유저", "encoded-password");
        setId(user, userId);

        Interest interest = new Interest("스포츠");
        setId(interest, interestId);
        interest.increaseSubscriberCount();

        Subscription subscription = new Subscription(user, interest);
        setId(subscription, subscriptionId);
        setCreatedAt(subscription, LocalDateTime.now());

        SubscriptionDto response = SubscriptionDto.of(
                subscription,
                interest,
                List.of("축구", "야구")
        );

        given(subscriptionService.subscribe(userId, interestId))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
                        .header(AUTH_HEADER, userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestName").value("스포츠"))
                .andExpect(jsonPath("$.interestKeywords[0]").value("축구"))
                .andExpect(jsonPath("$.interestKeywords[1]").value("야구"))
                .andExpect(jsonPath("$.interestSubscriberCount").value(1));

        verify(subscriptionService).subscribe(userId, interestId);
    }

    @Test
    @DisplayName("관심사 구독을 취소하면 200 OK를 반환한다")
    void unsubscribe_success() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID interestId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
                        .header(AUTH_HEADER, userId.toString()))
                .andExpect(status().isOk());

        verify(subscriptionService).unsubscribe(userId, interestId);
    }

    private void setId(Object target, UUID id) {
        ReflectionTestUtils.setField(target, "id", id);
    }

    private void setCreatedAt(Object target, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(target, "createdAt", createdAt);
    }
}