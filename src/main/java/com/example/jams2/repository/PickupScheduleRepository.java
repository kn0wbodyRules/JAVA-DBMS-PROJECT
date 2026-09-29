package com.example.jams2.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jams2.entity.PickupSchedule;

public interface PickupScheduleRepository extends JpaRepository<PickupSchedule, Long> {

    List<PickupSchedule> findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(DayOfWeek dayOfWeek);

    List<PickupSchedule> findByZoneId(Long zoneId);

    long countByZoneId(Long zoneId);

    long countByCollectorId(Long collectorId);
}
