package com.example.jams2.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.example.jams2.entity.Household;
import com.example.jams2.entity.PickupSchedule;
import com.example.jams2.entity.SegregationScore;
import com.example.jams2.entity.Zone;

// Runs against MySQL (waste_collection_test_db); each test rolls back
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SegregationScoreRepositoryTests {

    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private HouseholdRepository householdRepository;
    @Autowired
    private PickupScheduleRepository pickupScheduleRepository;
    @Autowired
    private SegregationScoreRepository segregationScoreRepository;

    private Household household(Zone zone, String number) {
        Household h = new Household();
        h.setHouseNumber(number);
        h.setOwnerName("Owner " + number);
        h.setZone(zone);
        return householdRepository.save(h);
    }

    private SegregationScore score(Household h, int value, LocalDate date) {
        SegregationScore s = new SegregationScore();
        s.setHousehold(h);
        s.setScore(value);
        s.setPickupDate(date);
        return segregationScoreRepository.save(s);
    }

    @Test
    void averagesAndHistoryOrdering() {
        Zone zone = new Zone();
        zone.setName("Repo Test Zone");
        zone = zoneRepository.save(zone);
        Household a = household(zone, "1");
        Household b = household(zone, "2");

        LocalDate today = LocalDate.now();
        score(a, 4, today.minusDays(2));
        score(a, 8, today);
        score(b, 6, today.minusDays(1));

        assertThat(segregationScoreRepository.overallAverageScore()).isEqualTo(6.0);
        assertThat(segregationScoreRepository.findByHouseholdIdOrderByPickupDateDescIdDesc(a.getId()))
                .extracting(SegregationScore::getScore)
                .containsExactly(8, 4);
        assertThat(householdRepository.countByZoneId(zone.getId())).isEqualTo(2);
    }

    @Test
    void findsTodaysActiveSlotsOnly() {
        Zone zone = new Zone();
        zone.setName("Schedule Zone");
        zone = zoneRepository.save(zone);

        PickupSchedule active = new PickupSchedule();
        active.setZone(zone);
        active.setDayOfWeek(DayOfWeek.WEDNESDAY);
        active.setWasteCategory("Dry");
        active.setStartTime(LocalTime.of(8, 0));
        active.setEndTime(LocalTime.of(10, 0));
        pickupScheduleRepository.save(active);

        PickupSchedule paused = new PickupSchedule();
        paused.setZone(zone);
        paused.setDayOfWeek(DayOfWeek.WEDNESDAY);
        paused.setWasteCategory("Wet");
        paused.setActive(false);
        pickupScheduleRepository.save(paused);

        assertThat(pickupScheduleRepository.findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(DayOfWeek.WEDNESDAY))
                .extracting(PickupSchedule::getWasteCategory)
                .containsExactly("Dry");
    }
}
