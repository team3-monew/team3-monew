package com.monew.server.subscription.controller;

import com.monew.server.common.security.LoginUser;
import com.monew.server.subscription.dto.SubscriptionDto;
import com.monew.server.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interests/{interestId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionDto> subscribe(@PathVariable UUID interestId,
                                                     @LoginUser UUID userId) {
        SubscriptionDto response = subscriptionService.subscribe(userId, interestId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(@PathVariable UUID interestId,
                                            @LoginUser UUID userId) {
        subscriptionService.unsubscribe(userId, interestId);

        return ResponseEntity.ok().build();
    }
}
