package com.opus.dispute.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidence_maps")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String claimId;

    private Long disputeId;

    @Column(columnDefinition = "TEXT")
    private String fetchPlan;

    @Column(columnDefinition = "TEXT")
    private String annotatedMap;

    @Column(columnDefinition = "TEXT")
    private String merchantSummary;

    @Column(columnDefinition = "TEXT")
    private String challengeRecommendation;

    @Column(columnDefinition = "TEXT")
    private String rebuttalDocument;

    private String enrichmentStatus;

    private String recommendationStrength;

    private LocalDateTime enrichmentRequestedAt;
    private LocalDateTime enrichmentCompletedAt;
    private LocalDateTime recommendationGeneratedAt;
    private LocalDateTime rebuttalGeneratedAt;
}
