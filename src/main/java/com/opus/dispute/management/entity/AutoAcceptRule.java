package com.opus.dispute.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "auto_accept_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoAcceptRule {

    @Id
    private String id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Boolean enabled;

    private Integer priority;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
