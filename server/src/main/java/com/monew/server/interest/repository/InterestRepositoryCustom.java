package com.monew.server.interest.repository;

import com.monew.server.interest.entity.Interest;

import java.time.LocalDateTime;
import java.util.List;

public interface InterestRepositoryCustom {

    List<Interest> searchInterests(
            String keyword,
            String orderBy,
            String direction,
            String cursor,
            LocalDateTime after,
            int limit
    );

    long countInterests(String keyword);
}
