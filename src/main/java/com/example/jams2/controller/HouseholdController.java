package com.example.jams2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jams2.entity.Household;
import com.example.jams2.service.FeedbackNoticeService;
import com.example.jams2.service.HouseholdService;
import com.example.jams2.service.SegregationScoreService;
import com.example.jams2.service.ZoneService;

@Controller
public class HouseholdController {

    private final HouseholdService householdService;
    private final ZoneService zoneService;
    private final SegregationScoreService segregationScoreService;
    private final FeedbackNoticeService feedbackNoticeService;

    public HouseholdController(HouseholdService householdService,
                               ZoneService zoneService,
                               SegregationScoreService segregationScoreService,
                               FeedbackNoticeService feedbackNoticeService) {
        this.householdService = householdService;
        this.zoneService = zoneService;
        this.segregationScoreService = segregationScoreService;
        this.feedbackNoticeService = feedbackNoticeService;
    }

    @GetMapping("/households")
    public String list(@RequestParam(required = false) Long zoneId, Model model) {
        model.addAttribute("activePage", "households");
        model.addAttribute("households", zoneId == null ? householdService.findAll() : householdService.findByZone(zoneId));
        model.addAttribute("zones", zoneService.findAll());
        model.addAttribute("selectedZoneId", zoneId);
        if (!model.containsAttribute("household")) {
            model.addAttribute("household", new Household());
        }
        return "households";
    }

    @PostMapping("/households/save")
    public String save(@ModelAttribute Household household,
                       @RequestParam(required = false) Long zoneId,
                       RedirectAttributes redirect) {
        household.setZone(zoneService.findById(zoneId));
        try {
            householdService.save(household);
            redirect.addFlashAttribute("success", "Household " + household.getHouseNumber() + " saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/households";
    }

    @GetMapping("/households/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Household household = householdService.findById(id);
        if (household == null) {
            return "redirect:/households";
        }
        model.addAttribute("activePage", "households");
        model.addAttribute("household", household);
        model.addAttribute("households", householdService.findAll());
        model.addAttribute("zones", zoneService.findAll());
        model.addAttribute("editing", true);
        return "households";
    }

    @GetMapping("/households/{id}")
    public String scorecard(@PathVariable Long id, Model model) {
        Household household = householdService.findById(id);
        if (household == null) {
            return "redirect:/households";
        }
        model.addAttribute("activePage", "households");
        model.addAttribute("household", household);
        model.addAttribute("summary", segregationScoreService.householdSummary(household));
        model.addAttribute("recentWindow", SegregationScoreService.RECENT_WINDOW);
        model.addAttribute("scores", segregationScoreService.findByHousehold(id));
        model.addAttribute("notices", feedbackNoticeService.findByHousehold(id));
        return "household_scorecard";
    }

    @GetMapping("/households/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        householdService.deleteById(id);
        redirect.addFlashAttribute("success", "Household and its score history deleted.");
        return "redirect:/households";
    }
}
