package com.docuflow.core.repository;

import com.docuflow.core.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    List<ProcessingJob> findByTenantIdAndStatusIn(UUID tenantId, List<JobStatus> statuses);
    
    List<ProcessingJob> findByDocumentId(UUID documentId);
    
    @Query("SELECT j FROM ProcessingJob j WHERE j.status = :status AND j.createdAt < :threshold ORDER BY j.createdAt ASC")
    List<ProcessingJob> findStuckJobs(@Param("status") JobStatus status, @Param("threshold") java.time.Instant threshold);
    
    Optional<ProcessingJob> findByDocumentIdAndStatusIn(UUID documentId, List<JobStatus> statuses);
}