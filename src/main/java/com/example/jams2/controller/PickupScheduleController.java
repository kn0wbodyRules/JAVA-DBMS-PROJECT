package com.example.jams2.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jams2.entity.PickupSchedule;
import com.example.jams2.service.CollectorService;
import com.example.jams2.service.PickupScheduleService;
import com.example.jams2.service.ZoneService;

@Controller
public class PickupScheduleController {

    private final PickupScheduleService pickupScheduleService;
    private final ZoneService zoneService;
    private final CollectorService collectorService;

    public PickupScheduleController(PickupScheduleService pickupScheduleService,
                                    ZoneService zoneService,
                                    CollectorService collectorService) {
        this.pickupScheduleService = pickupScheduleService;
        this.zoneService = zoneService;
        this.collectorService = collectorService;
    }

    private void addFormData(Model model) {
        model.addAttribute("activePage", "schedules");
        model.addAttribute("schedules", pickupScheduleService.findAll());
        model.addAttribute("zones", zoneService.findAll());
        model.addAttribute("collectors", collectorService.findActive());
        model.addAttribute("days", DayOfWeek.values());
        model.addAttribute("grid", pickupScheduleService.weeklyGrid());
        model.addAttribute("todayDay", LocalDate.now().getDayOfWeek());
    }

    @GetMapping("/schedules")
    public String list(Model model) {
        addFormData(model);
        if (!model.containsAttribute("schedule")) {
            model.addAttribute("schedule", new PickupSchedule());
        }
        return "schedules";
    }

    @PostMapping("/schedules/save")
    public String save(@ModelAttribute("schedule") PickupSchedule schedule,
                       @RequestParam(required = false) Long zoneId,
                       @RequestParam(required = false) Long collectorId,
                       RedirectAttributes redirect) {
        schedule.setZone(zoneService.findById(zoneId));
        schedule.setCollector(collectorService.findById(collectorId));
        try {
            pickupScheduleService.save(schedule);
            redirect.addFlashAttribute("success", "Pickup schedule saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/schedules";
    }

    @GetMapping("/schedules/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        PickupSchedule schedule = pickupScheduleService.findById(id);
        if (schedule == null) {
            return "redirect:/schedules";
        }
        addFormData(model);
        model.addAttribute("schedule", schedule);
        model.addAttribute("editing", true);
        return "schedules";
    }

    @GetMapping("/schedules/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        pickupScheduleService.deleteById(id);
        redirect.addFlashAttribute("success", "Pickup schedule deleted.");
        return "redirect:/schedules";
    }
}
