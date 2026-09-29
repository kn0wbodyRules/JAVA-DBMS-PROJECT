package com.example.jams2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jams2.entity.Household;

public interface HouseholdRepository extends JpaRepository<Household, Long> {

    List<Household> findByZoneIdOrderByHouseNumberAsc(Long zoneId);

    long countByZoneId(Long zoneId);
}
