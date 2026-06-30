package com.monew.server.interest.service;

import com.monew.server.common.response.CursorPageResponse;
import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public interface InterestService {

    InterestDto register(InterestRegisterRequest request);

    InterestDto update(UUID interestId, InterestUpdateRequest request);

    void delete(UUID interestId);

    CursorPageResponse<InterestDto> getInterests(
            String keyword,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit,
            UUID requestUserId
    );
}
