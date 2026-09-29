package com.example.jams2.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.jams2.entity.Zone;
import com.example.jams2.service.ZoneService;

@Controller
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping("/zones")
    public String list(Model model) {
        model.addAttribute("activePage", "zones");
        model.addAttribute("zones", zoneService.findAll());
        if (!model.containsAttribute("zone")) {
            model.addAttribute("zone", new Zone());
        }
        return "zones";
    }

    @PostMapping("/zones/save")
    public String save(@ModelAttribute Zone zone, RedirectAttributes redirect) {
        try {
            zoneService.save(zone);
            redirect.addFlashAttribute("success", "Zone '" + zone.getName() + "' saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirect.addFlashAttribute("error", "A zone with that name already exists.");
        }
        return "redirect:/zones";
    }

    @GetMapping("/zones/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Zone zone = zoneService.findById(id);
        if (zone == null) {
            return "redirect:/zones";
        }
        model.addAttribute("activePage", "zones");
        model.addAttribute("zone", zone);
        model.addAttribute("zones", zoneService.findAll());
        model.addAttribute("editing", true);
        return "zones";
    }

    @GetMapping("/zones/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            zoneService.deleteById(id);
            redirect.addFlashAttribute("success", "Zone deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/zones";
    }
}
