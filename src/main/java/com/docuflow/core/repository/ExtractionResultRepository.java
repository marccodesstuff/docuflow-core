package com.docuflow.core.repository;

import com.docuflow.core.entity.ExtractionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractionResultRepository extends JpaRepository<ExtractionResult, UUID> {

    Optional<ExtractionResult> findByJobId(UUID jobId);
    
    Optional<ExtractionResult> findByDocumentId(UUID documentId);
}