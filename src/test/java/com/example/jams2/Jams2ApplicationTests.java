package com.example.jams2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.example.jams2.entity.FeedbackNotice;
import com.example.jams2.entity.Household;
import com.example.jams2.entity.SegregationScore;
import com.example.jams2.entity.Zone;
import com.example.jams2.repository.CollectorRepository;
import com.example.jams2.repository.FeedbackNoticeRepository;
import com.example.jams2.repository.HouseholdRepository;
import com.example.jams2.repository.PickupScheduleRepository;
import com.example.jams2.repository.SegregationScoreRepository;
import com.example.jams2.repository.ZoneRepository;

/**
 * Drives the whole app through its HTTP endpoints against MySQL (waste_collection_test_db),
 * then checks the rows that landed in the tables.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Jams2ApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private CollectorRepository collectorRepository;
    @Autowired
    private HouseholdRepository householdRepository;
    @Autowired
    private PickupScheduleRepository pickupScheduleRepository;
    @Autowired
    private SegregationScoreRepository segregationScoreRepository;
    @Autowired
    private FeedbackNoticeRepository feedbackNoticeRepository;

    @BeforeEach
    void cleanTables() {
        feedbackNoticeRepository.deleteAll();
        segregationScoreRepository.deleteAll();
        pickupScheduleRepository.deleteAll();
        householdRepository.deleteAll();
        collectorRepository.deleteAll();
        zoneRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void everyPageRenders() throws Exception {
        for (String page : List.of("/", "/zones", "/households", "/collectors", "/schedules", "/scores", "/notices", "/reports")) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
    }

    @Test
    void fullScheduleScoreFeedbackFlow() throws Exception {
        // 1. Zone
        mvc.perform(post("/zones/save").param("name", "Ward 7 North").param("wardCode", "W-07"))
                .andExpect(redirectedUrl("/zones"));
        Zone zone = zoneRepository.findAll().get(0);

        // 2. Collector
        mvc.perform(post("/collectors/save").param("name", "Ravi Kumar").param("employeeCode", "COL-1")
                        .param("active", "true").param("zoneId", zone.getId().toString()))
                .andExpect(redirectedUrl("/collectors"));
        Long collectorId = collectorRepository.findAll().get(0).getId();

        // 3. Weekly pickup slot for the zone
        mvc.perform(post("/schedules/save").param("zoneId", zone.getId().toString())
                        .param("collectorId", collectorId.toString())
                        .param("dayOfWeek", "MONDAY").param("wasteCategory", "Wet")
                        .param("startTime", "07:00").param("endTime", "09:30").param("active", "true"))
                .andExpect(flash().attributeExists("success"));
        assertThat(pickupScheduleRepository.findByZoneId(zone.getId())).hasSize(1);

        // Invalid slot (end before start) is rejected
        mvc.perform(post("/schedules/save").param("zoneId", zone.getId().toString())
                        .param("dayOfWeek", "TUESDAY").param("wasteCategory", "Dry")
                        .param("startTime", "10:00").param("endTime", "09:00"))
                .andExpect(flash().attribute("error", "End time must be after start time."));
        assertThat(pickupScheduleRepository.count()).isEqualTo(1);

        // 4. Two households
        mvc.perform(post("/households/save").param("houseNumber", "12A").param("ownerName", "Good House")
                .param("zoneId", zone.getId().toString()).param("memberCount", "4"));
        mvc.perform(post("/households/save").param("houseNumber", "14B").param("ownerName", "Mixed House")
                .param("zoneId", zone.getId().toString()).param("memberCount", "3"));
        List<Household> households = householdRepository.findByZoneIdOrderByHouseNumberAsc(zone.getId());
        assertThat(households).hasSize(2);
        Household good = households.get(0);
        Household mixed = households.get(1);

        // 5. Collector logs scores
        String date = LocalDate.now().toString();
        mvc.perform(post("/scores/save").param("householdId", good.getId().toString())
                        .param("collectorId", collectorId.toString()).param("pickupDate", date).param("score", "9"))
                .andExpect(flash().attributeExists("success"));
        mvc.perform(post("/scores/save").param("householdId", mixed.getId().toString())
                .param("collectorId", collectorId.toString()).param("pickupDate", date)
                .param("score", "2").param("wetDryMixed", "true"));

        // Out-of-range and future-dated scores are rejected
        mvc.perform(post("/scores/save").param("householdId", good.getId().toString())
                        .param("pickupDate", date).param("score", "11"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post("/scores/save").param("householdId", good.getId().toString())
                        .param("pickupDate", LocalDate.now().plusDays(1).toString()).param("score", "5"))
                .andExpect(flash().attribute("error", "Pickup date cannot be in the future."));

        List<SegregationScore> scores = segregationScoreRepository.findAll();
        assertThat(scores).hasSize(2);
        assertThat(segregationScoreRepository.findByHouseholdIdOrderByPickupDateDescIdDesc(mixed.getId()).get(0).isWetDryMixed()).isTrue();

        // 6. Feedback loop: one notice per scored household, typed by average
        mvc.perform(post("/notices/generate")).andExpect(flash().attribute("success",
                "2 feedback notice(s) generated from recent scores."));
        FeedbackNotice goodNotice = feedbackNoticeRepository.findByHouseholdIdOrderByIssuedDateDescIdDesc(good.getId()).get(0);
        FeedbackNotice badNotice = feedbackNoticeRepository.findByHouseholdIdOrderByIssuedDateDescIdDesc(mixed.getId()).get(0);
        assertThat(goodNotice.getNoticeType()).isEqualTo("Appreciation");
        assertThat(badNotice.getNoticeType()).isEqualTo("Warning");
        assertThat(badNotice.getAverageScore()).isEqualTo(2.0);

        // Generating again the same day does not duplicate
        mvc.perform(post("/notices/generate"));
        assertThat(feedbackNoticeRepository.count()).isEqualTo(2);

        mvc.perform(get("/notices/acknowledge/" + badNotice.getId()));
        assertThat(feedbackNoticeRepository.findById(badNotice.getId()).orElseThrow().getStatus()).isEqualTo("Acknowledged");

        // 7. Pages show the new data
        mvc.perform(get("/households/" + mixed.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mixed House")));
        mvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ward 7 North")));
        mvc.perform(get("/")).andExpect(status().isOk());

        // 8. A zone in use cannot be deleted
        mvc.perform(get("/zones/delete/" + zone.getId())).andExpect(flash().attributeExists("error"));
        assertThat(zoneRepository.existsById(zone.getId())).isTrue();

        // 9. Deleting a household removes its scores and notices too
        mvc.perform(get("/households/delete/" + mixed.getId())).andExpect(redirectedUrl("/households"));
        assertThat(householdRepository.existsById(mixed.getId())).isFalse();
        assertThat(segregationScoreRepository.count()).isEqualTo(1);
        assertThat(feedbackNoticeRepository.count()).isEqualTo(1);
    }
}
