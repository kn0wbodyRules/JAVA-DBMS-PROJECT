package com.example.jams2.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jams2.entity.FeedbackNotice;
import com.example.jams2.repository.FeedbackNoticeRepository;
import com.example.jams2.service.SegregationScoreService.HouseholdSummary;

@Service
public class FeedbackNoticeService {

    private final FeedbackNoticeRepository feedbackNoticeRepository;
    private final SegregationScoreService segregationScoreService;

    public FeedbackNoticeService(FeedbackNoticeRepository feedbackNoticeRepository,
                                 SegregationScoreService segregationScoreService) {
        this.feedbackNoticeRepository = feedbackNoticeRepository;
        this.segregationScoreService = segregationScoreService;
    }

    public List<FeedbackNotice> findAll() {
        return feedbackNoticeRepository.findAllByOrderByIssuedDateDescIdDesc();
    }

    public List<FeedbackNotice> findByHousehold(Long householdId) {
        return feedbackNoticeRepository.findByHouseholdIdOrderByIssuedDateDescIdDesc(householdId);
    }

    public FeedbackNotice findById(Long id) {
        return id == null ? null : feedbackNoticeRepository.findById(id).orElse(null);
    }

    public long countPending() {
        return feedbackNoticeRepository.countByStatus("Issued");
    }

    public FeedbackNotice save(FeedbackNotice notice) {
        if (notice.getHousehold() == null) {
            throw new IllegalArgumentException("Please choose a household.");
        }
        if (notice.getIssuedDate() == null) {
            notice.setIssuedDate(LocalDate.now());
        }
        if (notice.getStatus() == null || notice.getStatus().isBlank()) {
            notice.setStatus("Issued");
        }
        return feedbackNoticeRepository.save(notice);
    }

    public void acknowledge(Long id) {
        FeedbackNotice notice = findById(id);
        if (notice != null) {
            notice.setStatus("Acknowledged");
            feedbackNoticeRepository.save(notice);
        }
    }

    public void deleteById(Long id) {
        feedbackNoticeRepository.deleteById(id);
    }

    // Closes the feedback loop: every scored household gets one notice per day based on its recent average
    @Transactional
    public int generateFromScores() {
        LocalDate today = LocalDate.now();
        int created = 0;
        for (HouseholdSummary summary : segregationScoreService.householdSummaries()) {
            Double avg = summary.getRecentAverage();
            Long householdId = summary.getHousehold().getId();
            if (avg == null || feedbackNoticeRepository.existsByHouseholdIdAndIssuedDate(householdId, today)) {
                continue;
            }
            FeedbackNotice notice = new FeedbackNotice();
            notice.setHousehold(summary.getHousehold());
            notice.setIssuedDate(today);
            notice.setAverageScore(avg);
            notice.setNoticeType(typeFor(avg));
            notice.setMessage(messageFor(summary));
            notice.setStatus("Issued");
            feedbackNoticeRepository.save(notice);
            created++;
        }
        return created;
    }

    static String typeFor(double avg) {
        if (avg >= 8) {
            return "Appreciation";
        }
        if (avg >= 5) {
            return "Reminder";
        }
        return "Warning";
    }

    private String messageFor(HouseholdSummary summary) {
        String house = summary.getHousehold().getHouseNumber();
        double avg = summary.getRecentAverage();
        String base = switch (typeFor(avg)) {
            case "Appreciation" -> "Thank you, house " + house + "! Your recent segregation average is " + avg
                    + "/10. Keep wet and dry waste separate as you have been doing.";
            case "Reminder" -> "House " + house + ": your recent segregation average is " + avg
                    + "/10. Please use separate bins for wet (kitchen) and dry (paper, plastic) waste.";
            default -> "House " + house + ": your recent segregation average is only " + avg
                    + "/10. Mixed waste cannot be processed. Repeated mixing may lead to pickup being refused.";
        };
        if ("Declining".equals(summary.getTrend())) {
            base += " Your scores have dropped compared to earlier pickups.";
        } else if ("Improving".equals(summary.getTrend())) {
            base += " Good progress compared to earlier pickups.";
        }
        return base;
    }
}
