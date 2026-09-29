package com.example.jams2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.jams2.entity.SegregationScore;

public interface SegregationScoreRepository extends JpaRepository<SegregationScore, Long> {

    List<SegregationScore> findAllByOrderByPickupDateDescIdDesc();

    List<SegregationScore> findByHouseholdIdOrderByPickupDateDescIdDesc(Long householdId);

    List<SegregationScore> findTop5ByOrderByPickupDateDescIdDesc();

    long countByCollectorId(Long collectorId);

    void deleteByHouseholdId(Long householdId);

    @Query("select avg(s.score) from SegregationScore s")
    Double overallAverageScore();
}
