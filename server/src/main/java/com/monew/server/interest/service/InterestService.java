package com.monew.server.interest.service;

import com.monew.server.interest.dto.InterestDto;
import com.monew.server.interest.dto.InterestRegisterRequest;
import com.monew.server.interest.dto.InterestUpdateRequest;

import java.util.UUID;

public interface InterestService {

    InterestDto register(InterestRegisterRequest request);

    InterestDto update(UUID interestId, InterestUpdateRequest request);

    void delete(UUID interestId);
}
