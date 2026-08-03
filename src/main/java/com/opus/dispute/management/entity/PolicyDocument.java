package com.opus.dispute.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_documents")
@Getter
@Setter
@NoArgsConstructor
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String policyType;

    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String filename;

    private String networkName;

    @Column(columnDefinition = "TEXT")
    private String diffSummary;

    @Column(columnDefinition = "TEXT")
    private String settingsRecommendations;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public PolicyDocument(String policyType, Integer version, String content, String filename) {
        this.policyType = policyType;
        this.version = version;
        this.content = content;
        this.filename = filename;
        this.uploadedAt = LocalDateTime.now();
    }
}
