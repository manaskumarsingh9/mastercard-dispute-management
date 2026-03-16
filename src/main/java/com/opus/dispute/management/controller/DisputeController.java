package com.opus.dispute.management.controller;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @PostMapping
    public ResponseEntity<Dispute> createDispute(@RequestBody Dispute dispute) {
        Dispute createdDispute = disputeService.createDispute(dispute);
        return new ResponseEntity<>(createdDispute, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Dispute>> getAllDisputes() {
        List<Dispute> disputes = disputeService.getAllDisputes();
        return new ResponseEntity<>(disputes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dispute> getDisputeById(@PathVariable Long id) {
        return disputeService.getDisputeById(id)
                .map(dispute -> new ResponseEntity<>(dispute, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
