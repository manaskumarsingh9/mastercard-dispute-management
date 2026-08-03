package com.opus.dispute.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_filing_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseFilingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    private String caseFilingStatus;
    private String caseId;
    private String caseType;
    private String claimId;
    private String claimType;
    private String currencyCode;
    private String customerFilingNumber;
    private String disputeAmount;
    private String dueDate;
    private String filingAgainstIca;
    private String filingAs;
    private String filingIca;
    private String merchantName;
    private String primaryAccountNum;
    private String violationCode;
    private String violationDate;
    private String rulingDate;
    private String rulingStatus;
    private String creditDate;
    private String chargebackDate;
    private String reasonCode;
    private String virtualAccountNum;

    @Column(columnDefinition = "TEXT")
    private String responseHistory;

    private LocalDateTime ingestedAt;
}
