package com.example.jams2.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jams2.entity.Household;
import com.example.jams2.repository.FeedbackNoticeRepository;
import com.example.jams2.repository.HouseholdRepository;
import com.example.jams2.repository.SegregationScoreRepository;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final SegregationScoreRepository segregationScoreRepository;
    private final FeedbackNoticeRepository feedbackNoticeRepository;

    public HouseholdService(HouseholdRepository householdRepository,
                            SegregationScoreRepository segregationScoreRepository,
                            FeedbackNoticeRepository feedbackNoticeRepository) {
        this.householdRepository = householdRepository;
        this.segregationScoreRepository = segregationScoreRepository;
        this.feedbackNoticeRepository = feedbackNoticeRepository;
    }

    public List<Household> findAll() {
        return householdRepository.findAll(Sort.by("zone.name", "houseNumber"));
    }

    public List<Household> findByZone(Long zoneId) {
        return householdRepository.findByZoneIdOrderByHouseNumberAsc(zoneId);
    }

    public Household findById(Long id) {
        return id == null ? null : householdRepository.findById(id).orElse(null);
    }

    public Household save(Household household) {
        if (household.getZone() == null) {
            throw new IllegalArgumentException("Please choose a zone for the household.");
        }
        if (household.getMemberCount() != null && household.getMemberCount() < 1) {
            throw new IllegalArgumentException("Member count must be at least 1.");
        }
        return householdRepository.save(household);
    }

    // Removing a household also removes its score history and notices
    @Transactional
    public void deleteById(Long id) {
        segregationScoreRepository.deleteByHouseholdId(id);
        feedbackNoticeRepository.deleteByHouseholdId(id);
        householdRepository.deleteById(id);
    }
}
