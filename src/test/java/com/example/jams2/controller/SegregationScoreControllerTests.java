package com.example.jams2.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.jams2.service.CollectorService;
import com.example.jams2.service.HouseholdService;
import com.example.jams2.service.SegregationScoreService;

@WebMvcTest(SegregationScoreController.class)
class SegregationScoreControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SegregationScoreService segregationScoreService;
    @MockitoBean
    private HouseholdService householdService;
    @MockitoBean
    private CollectorService collectorService;

    @Test
    void listPageRenders() throws Exception {
        when(segregationScoreService.findAll()).thenReturn(List.of());
        when(householdService.findAll()).thenReturn(List.of());
        when(collectorService.findActive()).thenReturn(List.of());

        mvc.perform(get("/scores"))
                .andExpect(status().isOk())
                .andExpect(view().name("scores"))
                .andExpect(model().attributeExists("score", "households", "collectors"));
    }

    @Test
    void validationErrorBecomesFlashMessage() throws Exception {
        when(segregationScoreService.save(any())).thenThrow(new IllegalArgumentException("Please choose a household."));

        mvc.perform(post("/scores/save").param("score", "5").param("pickupDate", "2026-01-01"))
                .andExpect(redirectedUrl("/scores"))
                .andExpect(flash().attribute("error", "Please choose a household."));
    }

    @Test
    void unknownIdOnEditRedirectsBack() throws Exception {
        mvc.perform(get("/scores/edit/999")).andExpect(redirectedUrl("/scores"));
        verify(segregationScoreService, never()).save(any());
    }
}
