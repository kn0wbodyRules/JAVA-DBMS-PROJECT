package com.example.jams2.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.jams2.entity.Collector;
import com.example.jams2.repository.CollectorRepository;
import com.example.jams2.repository.PickupScheduleRepository;
import com.example.jams2.repository.SegregationScoreRepository;

@Service
public class CollectorService {

    private final CollectorRepository collectorRepository;
    private final PickupScheduleRepository pickupScheduleRepository;
    private final SegregationScoreRepository segregationScoreRepository;

    public CollectorService(CollectorRepository collectorRepository,
                            PickupScheduleRepository pickupScheduleRepository,
                            SegregationScoreRepository segregationScoreRepository) {
        this.collectorRepository = collectorRepository;
        this.pickupScheduleRepository = pickupScheduleRepository;
        this.segregationScoreRepository = segregationScoreRepository;
    }

    public List<Collector> findAll() {
        return collectorRepository.findAll(Sort.by("name"));
    }

    public List<Collector> findActive() {
        return collectorRepository.findByActiveTrueOrderByNameAsc();
    }

    public Collector findById(Long id) {
        return id == null ? null : collectorRepository.findById(id).orElse(null);
    }

    public Collector save(Collector collector) {
        if (collector.getName() == null || collector.getName().isBlank()) {
            throw new IllegalArgumentException("Collector name is required.");
        }
        if (collector.getEmployeeCode() != null && collector.getEmployeeCode().isBlank()) {
            collector.setEmployeeCode(null);
        }
        return collectorRepository.save(collector);
    }

    public void deleteById(Long id) {
        long schedules = pickupScheduleRepository.countByCollectorId(id);
        long scores = segregationScoreRepository.countByCollectorId(id);
        if (schedules + scores > 0) {
            throw new IllegalStateException("This collector is linked to " + schedules + " schedule(s) and "
                    + scores + " score log(s). Mark them inactive instead of deleting.");
        }
        collectorRepository.deleteById(id);
    }
}
