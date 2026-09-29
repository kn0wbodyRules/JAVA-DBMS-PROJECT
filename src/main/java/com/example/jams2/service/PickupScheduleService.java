package com.example.jams2.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.example.jams2.entity.PickupSchedule;
import com.example.jams2.repository.PickupScheduleRepository;

@Service
public class PickupScheduleService {

    private final PickupScheduleRepository pickupScheduleRepository;

    public PickupScheduleService(PickupScheduleRepository pickupScheduleRepository) {
        this.pickupScheduleRepository = pickupScheduleRepository;
    }

    // Sorted Monday to Sunday, then by start time; the database would sort day names alphabetically
    public List<PickupSchedule> findAll() {
        return pickupScheduleRepository.findAll().stream()
                .sorted(Comparator.comparing(PickupSchedule::getDayOfWeek)
                        .thenComparing(PickupSchedule::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(s -> s.getZone().getName()))
                .toList();
    }

    // zone name -> day name -> schedules, used to draw the weekly timetable
    public Map<String, Map<String, List<PickupSchedule>>> weeklyGrid() {
        Map<String, Map<String, List<PickupSchedule>>> grid = new TreeMap<>();
        for (PickupSchedule schedule : findAll()) {
            if (!schedule.isActive()) {
                continue;
            }
            grid.computeIfAbsent(schedule.getZone().getName(), z -> new HashMap<>())
                    .computeIfAbsent(schedule.getDayOfWeek().name(), d -> new ArrayList<>())
                    .add(schedule);
        }
        return grid;
    }

    public List<PickupSchedule> findForDay(DayOfWeek day) {
        return pickupScheduleRepository.findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(day);
    }

    public List<PickupSchedule> findToday() {
        return findForDay(LocalDate.now().getDayOfWeek());
    }

    public PickupSchedule findById(Long id) {
        return id == null ? null : pickupScheduleRepository.findById(id).orElse(null);
    }

    public PickupSchedule save(PickupSchedule schedule) {
        if (schedule.getZone() == null) {
            throw new IllegalArgumentException("Please choose a zone for the schedule.");
        }
        if (schedule.getDayOfWeek() == null) {
            throw new IllegalArgumentException("Please choose a pickup day.");
        }
        LocalTime start = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        return pickupScheduleRepository.save(schedule);
    }

    public void deleteById(Long id) {
        pickupScheduleRepository.deleteById(id);
    }
}
