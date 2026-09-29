package com.example.jams2.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jams2.entity.FeedbackNotice;

public interface FeedbackNoticeRepository extends JpaRepository<FeedbackNotice, Long> {

    List<FeedbackNotice> findAllByOrderByIssuedDateDescIdDesc();

    List<FeedbackNotice> findByHouseholdIdOrderByIssuedDateDescIdDesc(Long householdId);

    boolean existsByHouseholdIdAndIssuedDate(Long householdId, LocalDate issuedDate);

    long countByStatus(String status);

    void deleteByHouseholdId(Long householdId);
}
