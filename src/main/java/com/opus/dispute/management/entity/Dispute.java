package com.opus.dispute.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disputes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String claimId;

    private String claimType;
    private String claimValue;
    private String primaryAccountNum;
    private String acquirerId;
    private String acquirerRefNum;
    private String issuerId;
    private String merchantId;
    private String transactionId;
    private String reasonCode;
    private String progressState;
    private String queueName;
    private String clearingNetwork;

    private Boolean isOpen;
    private Boolean isIssuer;
    private Boolean isAcquirer;
    private Boolean isAccurate;

    private String clearingDueDate;
    private String dueDate;
    private String createDate;
    private String lastModifiedBy;
    private String lastModifiedMcDate;

    private String status;
    private LocalDateTime ingestedAt;
    private LocalDateTime lastUpdatedDate;

    @Column(columnDefinition = "TEXT")
    private String issuerSummary;

    private String chargebackDisplayId;
    private String disputeType;
    private Double amount;
    private String currency;
    private String merchantName;
    private String cardholderName;
    private String reasonCodeDescription;
    private String itemDescription;
    private Integer enrichmentPercentage;
    private Integer sourcesCount;
    private String action;

    private String merchantCategory;
    private String customerEmail;
    private String cardNumber;
    private String evidenceFileId;

    private Boolean detailsFetched;
    private String auditControlNumber;
    private String switchSerialNumber;

    private String stripeDisputeId;
    private String stripeChargeId;
    private String stripePaymentIntentId;

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ChargebackDetail> chargebackDetails = new ArrayList<>();

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<RetrievalDetail> retrievalDetails = new ArrayList<>();

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<FeeDetail> feeDetails = new ArrayList<>();

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CaseFilingDetail> caseFilingDetails = new ArrayList<>();
}
