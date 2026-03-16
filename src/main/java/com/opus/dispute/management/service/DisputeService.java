package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DisputeService {

    @Autowired
    private DisputeRepository disputeRepository;

    public Dispute createDispute(Dispute dispute) {
        dispute.setClaimId(UUID.randomUUID().toString()); // Generate a unique claimId
        dispute.setCreatedDate(LocalDateTime.now());
        dispute.setLastUpdatedDate(LocalDateTime.now());
        dispute.setStatus("INITIATED");
        return disputeRepository.save(dispute);
    }

    public List<Dispute> getAllDisputes() {
        return disputeRepository.findAll();
    }

    public Optional<Dispute> getDisputeById(Long id) {
        return disputeRepository.findById(id);
    }
}
