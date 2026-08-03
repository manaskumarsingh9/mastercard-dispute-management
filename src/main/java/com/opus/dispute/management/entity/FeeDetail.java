package com.opus.dispute.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fee_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    private String feeId;
    private String cardAcceptorIdCode;
    private String cardNumber;
    private String countryCode;
    private String currency;
    private String feeDate;
    private String destinationMember;
    private String feeAmount;
    private Boolean creditSender;
    private Boolean creditReceiver;
    private String reason;
    private String chargebackRefNum;
    private String reconciliationAmount;
    private String reconciliationCurrency;
    private String rejectReason;
    @Column(columnDefinition = "TEXT")
    private String message;
    private String flexCode;

    private LocalDateTime ingestedAt;
}
