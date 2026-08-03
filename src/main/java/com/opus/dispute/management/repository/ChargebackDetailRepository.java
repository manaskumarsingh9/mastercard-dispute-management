package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.ChargebackDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChargebackDetailRepository extends JpaRepository<ChargebackDetail, Long> {
    List<ChargebackDetail> findByDisputeId(Long disputeId);
    List<ChargebackDetail> findByClaimId(String claimId);
    @Transactional
    void deleteByDisputeId(Long disputeId);
    @Transactional
    long deleteByChargebackType(String chargebackType);
}
