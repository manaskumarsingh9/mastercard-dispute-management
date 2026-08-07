package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final ReasonCodeRulesService reasonCodeRulesService;

    public DisputeService(DisputeRepository disputeRepository, ReasonCodeRulesService reasonCodeRulesService) {
        this.disputeRepository = disputeRepository;
        this.reasonCodeRulesService = reasonCodeRulesService;
    }

    public Dispute createDispute(Dispute dispute) {
        dispute.setClaimId(UUID.randomUUID().toString());
        dispute.setIngestedAt(LocalDateTime.now());
        dispute.setLastUpdatedDate(LocalDateTime.now());
        dispute.setStatus("INITIATED");
        return disputeRepository.save(dispute);
    }

    public List<Dispute> getAllDisputes() {
        return disputeRepository.findByReasonCodeIn(reasonCodeRulesService.getSupportedReasonCodes());
    }

    public Optional<Dispute> getDisputeById(Long id) {
        return disputeRepository.findById(id);
    }
}
