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

import com.example.jams2.entity.FeedbackNotice;
import com.example.jams2.service.FeedbackNoticeService;
import com.example.jams2.service.HouseholdService;

@Controller
public class FeedbackNoticeController {

    private final FeedbackNoticeService feedbackNoticeService;
    private final HouseholdService householdService;

    public FeedbackNoticeController(FeedbackNoticeService feedbackNoticeService,
                                    HouseholdService householdService) {
        this.feedbackNoticeService = feedbackNoticeService;
        this.householdService = householdService;
    }

    private void addFormData(Model model) {
        model.addAttribute("activePage", "notices");
        model.addAttribute("notices", feedbackNoticeService.findAll());
        model.addAttribute("households", householdService.findAll());
    }

    @GetMapping("/notices")
    public String list(Model model) {
        addFormData(model);
        if (!model.containsAttribute("notice")) {
            FeedbackNotice notice = new FeedbackNotice();
            notice.setIssuedDate(LocalDate.now());
            model.addAttribute("notice", notice);
        }
        return "notices";
    }

    @PostMapping("/notices/save")
    public String save(@ModelAttribute("notice") FeedbackNotice notice,
                       @RequestParam(required = false) Long householdId,
                       RedirectAttributes redirect) {
        notice.setHousehold(householdService.findById(householdId));
        try {
            feedbackNoticeService.save(notice);
            redirect.addFlashAttribute("success", "Notice saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notices";
    }

    @PostMapping("/notices/generate")
    public String generate(RedirectAttributes redirect) {
        int created = feedbackNoticeService.generateFromScores();
        redirect.addFlashAttribute("success", created == 0
                ? "No new notices: every scored household already has one for today."
                : created + " feedback notice(s) generated from recent scores.");
        return "redirect:/notices";
    }

    @GetMapping("/notices/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        FeedbackNotice notice = feedbackNoticeService.findById(id);
        if (notice == null) {
            return "redirect:/notices";
        }
        addFormData(model);
        model.addAttribute("notice", notice);
        model.addAttribute("editing", true);
        return "notices";
    }

    @GetMapping("/notices/acknowledge/{id}")
    public String acknowledge(@PathVariable Long id, RedirectAttributes redirect) {
        feedbackNoticeService.acknowledge(id);
        redirect.addFlashAttribute("success", "Notice marked as acknowledged.");
        return "redirect:/notices";
    }

    @GetMapping("/notices/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        feedbackNoticeService.deleteById(id);
        redirect.addFlashAttribute("success", "Notice deleted.");
        return "redirect:/notices";
    }
}
