package com.monew.batch.interest.repository;

import com.monew.batch.interest.entity.InterestKeyword;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestKeywordRepository extends JpaRepository<InterestKeyword, UUID> {

  @EntityGraph(attributePaths = "interest")
  List<InterestKeyword> findAll();
}
