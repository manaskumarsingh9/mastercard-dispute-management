package com.opus.dispute.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chargeback_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargebackDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    private String chargebackId;
    private String claimId;
    private String reasonCode;
    private String chargebackType;
    private String amount;
    private String currency;
    private String messageText;
    private Boolean isPartialChargeback;
    private Boolean reversed;
    private Boolean reversal;
    private String createDate;
    private String chargebackRefNum;
    private String documentStatus;
    private Boolean documentIndicator;
    private String reconciliationAmount;
    private String reconciliationCurrency;
    private String editExclusionCode;
    private String rejectReason;
    private Boolean refundNotReceivedIndicator;
    private String creditVoucherStatus;
    private Boolean currencyConversionAssessmentCCAIncluded;
    private String currencyConversionAssessmentCCAAmount;
    private String flexCode;

    private LocalDateTime ingestedAt;
}
