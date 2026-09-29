package com.example.jams2.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SegregationScoreTests {

    @Test
    void gradeBoundaries() {
        assertThat(SegregationScore.gradeFor(10.0)).isEqualTo("Excellent");
        assertThat(SegregationScore.gradeFor(8.0)).isEqualTo("Excellent");
        assertThat(SegregationScore.gradeFor(7.9)).isEqualTo("Fair");
        assertThat(SegregationScore.gradeFor(5.0)).isEqualTo("Fair");
        assertThat(SegregationScore.gradeFor(4.9)).isEqualTo("Poor");
        assertThat(SegregationScore.gradeFor(0.0)).isEqualTo("Poor");
        assertThat(SegregationScore.gradeFor(null)).isEqualTo("N/A");
    }
}
