package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.IngestionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionStateRepository extends JpaRepository<IngestionState, String> {
}
