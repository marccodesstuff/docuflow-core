package com.docuflow.core.repository;

import com.docuflow.core.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {

    List<DocumentType> findByTenantId(UUID tenantId);
    
    Optional<DocumentType> findByTenantIdAndName(UUID tenantId, String name);
}