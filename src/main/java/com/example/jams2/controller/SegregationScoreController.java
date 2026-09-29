package com.example.jams2.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jams2.entity.SegregationScore;
import com.example.jams2.service.CollectorService;
import com.example.jams2.service.HouseholdService;
import com.example.jams2.service.SegregationScoreService;

@Controller
public class SegregationScoreController {

    private final SegregationScoreService segregationScoreService;
    private final HouseholdService householdService;
    private final CollectorService collectorService;

    public SegregationScoreController(SegregationScoreService segregationScoreService,
                                      HouseholdService householdService,
                                      CollectorService collectorService) {
        this.segregationScoreService = segregationScoreService;
        this.householdService = householdService;
        this.collectorService = collectorService;
    }

    private void addFormData(Model model) {
        model.addAttribute("activePage", "scores");
        model.addAttribute("scores", segregationScoreService.findAll());
        model.addAttribute("households", householdService.findAll());
        model.addAttribute("collectors", collectorService.findActive());
        model.addAttribute("today", LocalDate.now());
    }

    @GetMapping("/scores")
    public String list(@RequestParam(required = false) Long householdId, Model model) {
        addFormData(model);
        if (!model.containsAttribute("score")) {
            SegregationScore score = new SegregationScore();
            score.setPickupDate(LocalDate.now());
            score.setHousehold(householdService.findById(householdId));
            model.addAttribute("score", score);
        }
        return "scores";
    }

    @PostMapping("/scores/save")
    public String save(@ModelAttribute("score") SegregationScore score,
                       @RequestParam(required = false) Long householdId,
                       @RequestParam(required = false) Long collectorId,
                       RedirectAttributes redirect) {
        score.setHousehold(householdService.findById(householdId));
        score.setCollector(collectorService.findById(collectorId));
        try {
            segregationScoreService.save(score);
            redirect.addFlashAttribute("success", "Score " + score.getScore() + "/10 logged for house "
                    + score.getHousehold().getHouseNumber() + ".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/scores";
    }

    @GetMapping("/scores/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        SegregationScore score = segregationScoreService.findById(id);
        if (score == null) {
            return "redirect:/scores";
        }
        addFormData(model);
        model.addAttribute("score", score);
        model.addAttribute("editing", true);
        return "scores";
    }

    @GetMapping("/scores/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        segregationScoreService.deleteById(id);
        redirect.addFlashAttribute("success", "Score log deleted.");
        return "redirect:/scores";
    }
}
