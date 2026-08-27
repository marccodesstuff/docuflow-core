package com.docuflow.core.repository;

import com.docuflow.core.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    
    List<Document> findByTenantIdAndStatusIn(UUID tenantId, List<DocumentStatus> statuses);
    
    List<Document> findByDocumentTypeId(UUID documentTypeId);
    
    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status ORDER BY d.createdAt ASC")
    List<Document> findOldestByStatus(@Param("tenantId") UUID tenantId, @Param("status") DocumentStatus status, org.springframework.data.domain.Pageable pageable);
    
    long countByTenantIdAndStatus(UUID tenantId, DocumentStatus status);
}