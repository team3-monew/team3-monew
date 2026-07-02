package com.monew.server.interest.controller;

import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.common.security.LoginUser;
import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;
import com.monew.server.interest.service.InterestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @PostMapping
    public ResponseEntity<InterestDto> register(
            @Valid @RequestBody InterestRegisterRequest request) {
        InterestDto response = interestService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{interestId}")
    public ResponseEntity<InterestDto> update(
            @PathVariable UUID interestId,
            @Valid @RequestBody InterestUpdateRequest request) {
        InterestDto response = interestService.update(interestId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{interestId}")
    public ResponseEntity<Void> delete(@PathVariable UUID interestId) {
        interestService.delete(interestId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<InterestDto>> getInterest(
            @RequestParam(required = false) String keyword,
            @RequestParam String orderBy,
            @RequestParam String direction,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime after,
            @RequestParam int limit,
            @LoginUser UUID userId
    ) {
        CursorPageResponse<InterestDto> response = interestService.getInterests(
                keyword,
                orderBy,
                direction,
                cursor,
                after,
                limit,
                userId
        );

        return ResponseEntity.ok(response);
    }
}
