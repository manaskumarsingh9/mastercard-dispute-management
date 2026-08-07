package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByClaimId(String claimId);

    boolean existsByClaimId(String claimId);

    List<Dispute> findByReasonCodeIn(Collection<String> reasonCodes);

    Page<Dispute> findByReasonCodeIn(Collection<String> reasonCodes, Pageable pageable);
}
