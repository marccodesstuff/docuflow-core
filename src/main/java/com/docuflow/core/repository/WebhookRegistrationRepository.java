package com.docuflow.core.repository;

import com.docuflow.core.entity.WebhookRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookRegistrationRepository extends JpaRepository<WebhookRegistration, UUID> {

    List<WebhookRegistration> findByTenantId(UUID tenantId);
    
    List<WebhookRegistration> findByTenantIdAndActiveTrue(UUID tenantId);
    
    List<WebhookRegistration> findByEventType(WebhookEventType eventType);
}