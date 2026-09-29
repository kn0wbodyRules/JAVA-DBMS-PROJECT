package com.example.jams2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.jams2.service.SegregationScoreService;

@Controller
public class ReportController {

    private final SegregationScoreService segregationScoreService;

    public ReportController(SegregationScoreService segregationScoreService) {
        this.segregationScoreService = segregationScoreService;
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("zoneSummaries", segregationScoreService.zoneSummaries());
        model.addAttribute("householdSummaries", segregationScoreService.householdSummaries());
        model.addAttribute("collectorSummaries", segregationScoreService.collectorSummaries());
        model.addAttribute("overallAverage", segregationScoreService.overallAverage());
        return "reports";
    }
}
