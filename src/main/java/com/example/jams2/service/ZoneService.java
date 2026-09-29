package com.example.jams2.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.jams2.entity.Zone;
import com.example.jams2.repository.CollectorRepository;
import com.example.jams2.repository.HouseholdRepository;
import com.example.jams2.repository.PickupScheduleRepository;
import com.example.jams2.repository.ZoneRepository;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final HouseholdRepository householdRepository;
    private final CollectorRepository collectorRepository;
    private final PickupScheduleRepository pickupScheduleRepository;

    public ZoneService(ZoneRepository zoneRepository,
                       HouseholdRepository householdRepository,
                       CollectorRepository collectorRepository,
                       PickupScheduleRepository pickupScheduleRepository) {
        this.zoneRepository = zoneRepository;
        this.householdRepository = householdRepository;
        this.collectorRepository = collectorRepository;
        this.pickupScheduleRepository = pickupScheduleRepository;
    }

    public List<Zone> findAll() {
        return zoneRepository.findAll(Sort.by("name"));
    }

    public Zone findById(Long id) {
        return id == null ? null : zoneRepository.findById(id).orElse(null);
    }

    public Zone save(Zone zone) {
        if (zone.getName() == null || zone.getName().isBlank()) {
            throw new IllegalArgumentException("Zone name is required.");
        }
        zone.setName(zone.getName().trim());
        return zoneRepository.save(zone);
    }

    public long householdCount(Long zoneId) {
        return householdRepository.countByZoneId(zoneId);
    }

    public void deleteById(Long id) {
        long households = householdRepository.countByZoneId(id);
        long collectors = collectorRepository.countByZoneId(id);
        long schedules = pickupScheduleRepository.countByZoneId(id);
        if (households + collectors + schedules > 0) {
            throw new IllegalStateException("This zone still has " + households + " household(s), "
                    + collectors + " collector(s) and " + schedules
                    + " schedule(s). Reassign or remove them first.");
        }
        zoneRepository.deleteById(id);
    }
}
