package com.example.jams2.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.jams2.entity.Collector;
import com.example.jams2.entity.Household;
import com.example.jams2.entity.SegregationScore;
import com.example.jams2.entity.Zone;
import com.example.jams2.repository.SegregationScoreRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Service
public class SegregationScoreService {

    // How many recent pickups count towards a household's current standing
    public static final int RECENT_WINDOW = 5;

    private final SegregationScoreRepository segregationScoreRepository;
    private final HouseholdService householdService;
    private final ZoneService zoneService;
    private final CollectorService collectorService;

    public SegregationScoreService(SegregationScoreRepository segregationScoreRepository,
                                   HouseholdService householdService,
                                   ZoneService zoneService,
                                   CollectorService collectorService) {
        this.segregationScoreRepository = segregationScoreRepository;
        this.householdService = householdService;
        this.zoneService = zoneService;
        this.collectorService = collectorService;
    }

    public List<SegregationScore> findAll() {
        return segregationScoreRepository.findAllByOrderByPickupDateDescIdDesc();
    }

    public List<SegregationScore> findRecent() {
        return segregationScoreRepository.findTop5ByOrderByPickupDateDescIdDesc();
    }

    public List<SegregationScore> findByHousehold(Long householdId) {
        return segregationScoreRepository.findByHouseholdIdOrderByPickupDateDescIdDesc(householdId);
    }

    public SegregationScore findById(Long id) {
        return id == null ? null : segregationScoreRepository.findById(id).orElse(null);
    }

    public SegregationScore save(SegregationScore score) {
        if (score.getHousehold() == null) {
            throw new IllegalArgumentException("Please choose a household.");
        }
        if (score.getScore() == null
                || score.getScore() < SegregationScore.MIN_SCORE
                || score.getScore() > SegregationScore.MAX_SCORE) {
            throw new IllegalArgumentException("Score must be between " + SegregationScore.MIN_SCORE
                    + " and " + SegregationScore.MAX_SCORE + ".");
        }
        if (score.getPickupDate() == null) {
            throw new IllegalArgumentException("Pickup date is required.");
        }
        if (score.getPickupDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Pickup date cannot be in the future.");
        }
        return segregationScoreRepository.save(score);
    }

    public void deleteById(Long id) {
        segregationScoreRepository.deleteById(id);
    }

    public long count() {
        return segregationScoreRepository.count();
    }

    public Double overallAverage() {
        return round(segregationScoreRepository.overallAverageScore());
    }

    // Per household scorecard, worst performers first
    public List<HouseholdSummary> householdSummaries() {
        Map<Long, List<SegregationScore>> byHousehold = findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getHousehold().getId(), LinkedHashMap::new, Collectors.toList()));

        List<HouseholdSummary> summaries = new ArrayList<>();
        for (Household household : householdService.findAll()) {
            summaries.add(summarise(household, byHousehold.getOrDefault(household.getId(), List.of())));
        }
        summaries.sort(Comparator.comparing(HouseholdSummary::getRecentAverage,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return summaries;
    }

    public HouseholdSummary householdSummary(Household household) {
        return summarise(household, findByHousehold(household.getId()));
    }

    // Scores must be sorted newest first
    private HouseholdSummary summarise(Household household, List<SegregationScore> scores) {
        if (scores.isEmpty()) {
            return new HouseholdSummary(household, 0, null, null, null, 0, "N/A", "No data");
        }
        List<SegregationScore> recent = scores.subList(0, Math.min(RECENT_WINDOW, scores.size()));
        List<SegregationScore> earlier = scores.subList(recent.size(), Math.min(recent.size() * 2, scores.size()));

        Double overall = average(scores);
        Double recentAvg = average(recent);
        int mixed = (int) scores.stream().filter(SegregationScore::isWetDryMixed).count();

        String trend = "Steady";
        if (!earlier.isEmpty()) {
            double diff = recentAvg - average(earlier);
            if (diff >= 1) {
                trend = "Improving";
            } else if (diff <= -1) {
                trend = "Declining";
            }
        }
        return new HouseholdSummary(household, scores.size(), overall, recentAvg,
                scores.get(0).getScore(), mixed, SegregationScore.gradeFor(recentAvg), trend);
    }

    // Compliance rate = share of pickups where wet and dry waste were kept apart
    public List<ZoneSummary> zoneSummaries() {
        Map<Long, List<SegregationScore>> byZone = findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getHousehold().getZone().getId()));

        List<ZoneSummary> summaries = new ArrayList<>();
        for (Zone zone : zoneService.findAll()) {
            List<SegregationScore> scores = byZone.getOrDefault(zone.getId(), List.of());
            long compliant = scores.stream().filter(s -> !s.isWetDryMixed()).count();
            Double compliance = scores.isEmpty() ? null : round(compliant * 100.0 / scores.size());
            summaries.add(new ZoneSummary(zone, zoneService.householdCount(zone.getId()), scores.size(),
                    average(scores), compliance));
        }
        return summaries;
    }

    public List<CollectorSummary> collectorSummaries() {
        Map<Long, List<SegregationScore>> byCollector = findAll().stream()
                .filter(s -> s.getCollector() != null)
                .collect(Collectors.groupingBy(s -> s.getCollector().getId()));

        List<CollectorSummary> summaries = new ArrayList<>();
        for (Collector collector : collectorService.findAll()) {
            List<SegregationScore> scores = byCollector.getOrDefault(collector.getId(), List.of());
            summaries.add(new CollectorSummary(collector, scores.size(), average(scores)));
        }
        return summaries;
    }

    private static Double average(List<SegregationScore> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        return round(scores.stream().mapToInt(SegregationScore::getScore).average().orElse(0));
    }

    private static Double round(Double value) {
        return value == null ? null : Math.round(value * 10) / 10.0;
    }

    @Getter
    @AllArgsConstructor
    public static class HouseholdSummary {
        private final Household household;
        private final int pickups;
        private final Double overallAverage;
        private final Double recentAverage;
        private final Integer latestScore;
        private final int mixedCount;
        private final String grade;
        private final String trend;
    }

    @Getter
    @AllArgsConstructor
    public static class ZoneSummary {
        private final Zone zone;
        private final long households;
        private final int pickups;
        private final Double averageScore;
        private final Double compliancePercent;
    }

    @Getter
    @AllArgsConstructor
    public static class CollectorSummary {
        private final Collector collector;
        private final int logs;
        private final Double averageGiven;
    }
}
