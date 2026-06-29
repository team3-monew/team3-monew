package com.monew.server.interest.repository;

import com.monew.server.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

    boolean existsByName(String name);

    @Query("select i.name from Interest i")
    List<String> findAllNames();
}
