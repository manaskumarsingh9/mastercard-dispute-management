package com.opus.dispute.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "retrieval_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    private String requestId;
    private String claimId;
    private String acquirerRefNum;
    private String acquirerResponseCd;
    private String acquirerMemo;
    private String acquirerResponseDt;
    private String issuerResponseCd;
    private String issuerRejectRsnCd;
    private String issuerMemo;
    private String issuerResponseDt;
    private String amount;
    private String currency;
    private String retrievalRequestReason;
    private String docNeeded;
    private String createDate;
    private String chargebackRefNum;
    private String cancelDate;
    private String rejectDate;
    private String reverseDate;
    private String acquirerResponseNotificationStatus;
    private String rejectReason;
    private String imageReviewDecision;
    private String imageReviewDt;
    private String collaborationExpirationDateTime;
    private String flexCode;

    private LocalDateTime ingestedAt;
}
