package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByClaimId(String claimId);

    boolean existsByClaimId(String claimId);
}
