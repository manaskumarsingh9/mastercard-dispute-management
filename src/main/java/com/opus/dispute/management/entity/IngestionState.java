package com.opus.dispute.management.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingestion_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionState {

    @Id
    private String id;

    private LocalDateTime lastPolledFrom;
    private LocalDateTime lastPolledTo;
    private LocalDateTime lastRunAt;
    private int claimsIngested;
    private int totalRuns;
}
