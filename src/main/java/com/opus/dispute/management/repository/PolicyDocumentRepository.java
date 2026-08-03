package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    Optional<PolicyDocument> findTopByPolicyTypeOrderByVersionDesc(String policyType);

    List<PolicyDocument> findByPolicyTypeOrderByVersionDesc(String policyType);

    Optional<PolicyDocument> findByPolicyTypeAndVersion(String policyType, Integer version);

    @Query("SELECT DISTINCT p.networkName FROM PolicyDocument p WHERE p.networkName IS NOT NULL ORDER BY p.networkName")
    List<String> findDistinctNetworkNames();
}
