package com.example.jams2.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jams2.entity.Zone;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    boolean existsByNameIgnoreCase(String name);
}
