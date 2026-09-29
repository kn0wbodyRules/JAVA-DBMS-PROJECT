package com.example.jams2.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jams2.entity.Collector;
import com.example.jams2.service.CollectorService;
import com.example.jams2.service.ZoneService;

@Controller
public class CollectorController {

    private final CollectorService collectorService;
    private final ZoneService zoneService;

    public CollectorController(CollectorService collectorService, ZoneService zoneService) {
        this.collectorService = collectorService;
        this.zoneService = zoneService;
    }

    @GetMapping("/collectors")
    public String list(Model model) {
        model.addAttribute("activePage", "collectors");
        model.addAttribute("collectors", collectorService.findAll());
        model.addAttribute("zones", zoneService.findAll());
        if (!model.containsAttribute("collector")) {
            model.addAttribute("collector", new Collector());
        }
        return "collectors";
    }

    @PostMapping("/collectors/save")
    public String save(@ModelAttribute Collector collector,
                       @RequestParam(required = false) Long zoneId,
                       RedirectAttributes redirect) {
        collector.setZone(zoneService.findById(zoneId));
        try {
            collectorService.save(collector);
            redirect.addFlashAttribute("success", "Collector '" + collector.getName() + "' saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "Employee code '" + collector.getEmployeeCode() + "' is already in use.");
        }
        return "redirect:/collectors";
    }

    @GetMapping("/collectors/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Collector collector = collectorService.findById(id);
        if (collector == null) {
            return "redirect:/collectors";
        }
        model.addAttribute("activePage", "collectors");
        model.addAttribute("collector", collector);
        model.addAttribute("collectors", collectorService.findAll());
        model.addAttribute("zones", zoneService.findAll());
        model.addAttribute("editing", true);
        return "collectors";
    }

    @GetMapping("/collectors/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            collectorService.deleteById(id);
            redirect.addFlashAttribute("success", "Collector deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/collectors";
    }
}
