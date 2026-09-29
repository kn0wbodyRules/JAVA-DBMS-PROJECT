package com.example.jams2.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jams2.entity.Collector;

public interface CollectorRepository extends JpaRepository<Collector, Long> {

    List<Collector> findByActiveTrueOrderByNameAsc();

    long countByZoneId(Long zoneId);
}
