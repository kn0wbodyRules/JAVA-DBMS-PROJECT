package com.example.jams2.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.jams2.service.CollectorService;
import com.example.jams2.service.FeedbackNoticeService;
import com.example.jams2.service.HouseholdService;
import com.example.jams2.service.PickupScheduleService;
import com.example.jams2.service.SegregationScoreService;
import com.example.jams2.service.SegregationScoreService.HouseholdSummary;
import com.example.jams2.service.ZoneService;

@Controller
public class DashboardController {

    private final ZoneService zoneService;
    private final HouseholdService householdService;
    private final CollectorService collectorService;
    private final PickupScheduleService pickupScheduleService;
    private final SegregationScoreService segregationScoreService;
    private final FeedbackNoticeService feedbackNoticeService;

    public DashboardController(ZoneService zoneService,
                               HouseholdService householdService,
                               CollectorService collectorService,
                               PickupScheduleService pickupScheduleService,
                               SegregationScoreService segregationScoreService,
                               FeedbackNoticeService feedbackNoticeService) {
        this.zoneService = zoneService;
        this.householdService = householdService;
        this.collectorService = collectorService;
        this.pickupScheduleService = pickupScheduleService;
        this.segregationScoreService = segregationScoreService;
        this.feedbackNoticeService = feedbackNoticeService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<HouseholdSummary> attention = segregationScoreService.householdSummaries().stream()
                .filter(s -> s.getRecentAverage() != null && s.getRecentAverage() < 5)
                .limit(5)
                .toList();

        model.addAttribute("activePage", "dashboard");
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("zoneCount", zoneService.findAll().size());
        model.addAttribute("householdCount", householdService.findAll().size());
        model.addAttribute("collectorCount", collectorService.findActive().size());
        model.addAttribute("scoreCount", segregationScoreService.count());
        model.addAttribute("averageScore", segregationScoreService.overallAverage());
        model.addAttribute("pendingNotices", feedbackNoticeService.countPending());
        model.addAttribute("todaySchedules", pickupScheduleService.findToday());
        model.addAttribute("recentScores", segregationScoreService.findRecent());
        model.addAttribute("attention", attention);
        return "index";
    }
}
