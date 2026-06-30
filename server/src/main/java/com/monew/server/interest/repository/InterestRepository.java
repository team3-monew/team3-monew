package com.monew.server.interest.repository;

import com.monew.server.interest.entity.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID>, InterestRepositoryCustom {

    boolean existsByName(String name);

    @Query("select i.name from Interest i")
    List<String> findAllNames();

    // 성능 개선: pg_trgm GIN index를 활용해 유사 후보만 조회
    @Query(value = """
        select name
        from interests
        where normalized_name % :normalizedName
    """, nativeQuery = true)
    List<String> findSimilarNameCandidates(@Param("normalizedName") String normalizedName);
}
