package com.docuflow.core.service;

import com.docuflow.core.entity.Document;
import com.docuflow.core.repository.DocumentRepository;
import com.docuflow.core.repository.DocumentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentTypeRepository documentTypeRepository) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
    }

    public Document createDocument(UUID tenantId, UUID documentTypeId, String originalFilename,
                                    String storagePath, String mimeType, Long sizeBytes) {
        Document document = new Document();
        document.setTenantId(tenantId);
        document.setDocumentTypeId(documentTypeId);
        document.setOriginalFilename(originalFilename);
        document.setStoragePath(storagePath);
        document.setMimeType(mimeType);
        document.setSizeBytes(sizeBytes);
        document.setStatus(DocumentStatus.UPLOADED);
        return documentRepository.save(document);
    }

    public Optional<Document> findByIdAndTenant(UUID id, UUID tenantId) {
        return documentRepository.findById(id).filter(d -> d.getTenantId().equals(tenantId));
    }

    public List<Document> findByTenantId(UUID tenantId) {
        return documentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<Document> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status) {
        return documentRepository.findByTenantIdAndStatusIn(tenantId, List.of(status));
    }

    public Document updateStatus(UUID id, UUID tenantId, DocumentStatus status) {
        Document document = documentRepository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        document.setStatus(status);
        if (status == DocumentStatus.APPROVED || status == DocumentStatus.REJECTED) {
            document.setProcessedAt(java.time.Instant.now());
        }
        return documentRepository.save(document);
    }

    public void delete(UUID id, UUID tenantId) {
        documentRepository.findById(id)
            .filter(d -> d.getTenantId().equals(tenantId))
            .ifPresent(documentRepository::delete);
    }
}