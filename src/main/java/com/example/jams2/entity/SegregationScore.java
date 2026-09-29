package com.example.jams2.entity;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "segregation_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegregationScore {

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne
    @JoinColumn(name = "collector_id")
    private Collector collector;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate pickupDate;

    // 0 = fully mixed, 10 = perfectly segregated
    @Column(nullable = false)
    private Integer score;

    private boolean wetDryMixed;
    private boolean plasticInWetWaste;
    private boolean hazardousNotSeparated;

    private String remarks;

    @Transient
    public String getGrade() {
        return gradeFor(score == null ? null : score.doubleValue());
    }

    public static String gradeFor(Double score) {
        if (score == null) {
            return "N/A";
        }
        if (score >= 8) {
            return "Excellent";
        }
        if (score >= 5) {
            return "Fair";
        }
        return "Poor";
    }
}
