package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.AutoAcceptRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutoAcceptRuleRepository extends JpaRepository<AutoAcceptRule, String> {
    List<AutoAcceptRule> findByEnabledTrueOrderByPriorityAsc();
}
