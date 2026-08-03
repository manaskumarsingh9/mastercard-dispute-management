package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.EvidenceMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvidenceMapRepository extends JpaRepository<EvidenceMap, Long> {
    Optional<EvidenceMap> findByClaimId(String claimId);
    Optional<EvidenceMap> findByDisputeId(Long disputeId);
}
