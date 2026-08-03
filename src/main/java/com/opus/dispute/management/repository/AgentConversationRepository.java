package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.AgentConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentConversationRepository extends JpaRepository<AgentConversation, Long> {

    List<AgentConversation> findByClaimIdAndAgentNameOrderByCreatedAtAsc(String claimId, String agentName);

    void deleteByClaimIdAndAgentName(String claimId, String agentName);
}
