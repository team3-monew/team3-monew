package com.monew.server.interest.repository;

import com.monew.server.interest.entity.Interest;
import com.monew.server.interest.entity.InterestKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

    void deleteByInterest(Interest interest);

    List<InterestKeyword> findAllByInterestId(UUID interestId);

    List<InterestKeyword> findAllByInterestIdIn(Collection<UUID> interestIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from InterestKeyword ik where ik.interest.id = :interestId")
    void deleteByInterestId(@Param("interestId") UUID interestId);
}
