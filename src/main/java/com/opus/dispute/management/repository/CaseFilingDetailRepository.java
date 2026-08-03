package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.CaseFilingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CaseFilingDetailRepository extends JpaRepository<CaseFilingDetail, Long> {
    List<CaseFilingDetail> findByDisputeId(Long disputeId);
    @Transactional
    void deleteByDisputeId(Long disputeId);
}
