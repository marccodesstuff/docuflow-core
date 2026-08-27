package com.docuflow.core.grpc;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.*;
import com.docuflow.core.service.DocumentService;
import com.docuflow.shared.proto.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class DocumentGrpcService extends DocumentServiceGrpc.DocumentServiceImplBase {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final DocumentService documentService;

    @Autowired
    public DocumentGrpcService(DocumentRepository documentRepository,
                                DocumentTypeRepository documentTypeRepository,
                                DocumentService documentService) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.documentService = documentService;
    }

    @Override
    @Transactional
    public void createDocument(CreateDocumentRequest request, StreamObserver<Document> responseObserver) {
        try {
            Document document = new Document();
            document.setTenantId(UUID.fromString(request.getTenantId()));
            document.setDocumentTypeId(UUID.fromString(request.getDocumentTypeId()));
            document.setOriginalFilename(request.getOriginalFilename());
            document.setStoragePath(request.getStoragePath());
            document.setMimeType(request.getMimeType());
            document.setSizeBytes(request.getSizeBytes());
            document.setStatus(DocumentStatus.UPLOADED);
            
            if (!request.getMetadataMap().isEmpty()) {
                document.setMetadata(toJson(request.getMetadataMap()));
            }
            
            Document saved = documentRepository.save(document);
            responseObserver.onNext(toProto(saved));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getDocument(GetDocumentRequest request, StreamObserver<Document> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getId());
            UUID tenantId = UUID.fromString(request.getTenantId());
            
            documentRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId))
                .ifPresentOrElse(
                    d -> responseObserver.onNext(toProto(d)),
                    () -> responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("Document not found").asRuntimeException())
                );
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional
    public void updateDocument(UpdateDocumentRequest request, StreamObserver<Document> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getId());
            UUID tenantId = UUID.fromString(request.getTenantId());
            
            documentRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId))
                .ifPresentOrElse(d -> {
                    if (request.hasStatus()) {
                        d.setStatus(convertStatus(request.getStatus()));
                    }
                    if (!request.getMetadataMap().isEmpty()) {
                        d.setMetadata(toJson(request.getMetadataMap()));
                    }
                    Document saved = documentRepository.save(d);
                    responseObserver.onNext(toProto(saved));
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Document not found").asRuntimeException()));
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional
    public void deleteDocument(DeleteDocumentRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getId());
            UUID tenantId = UUID.fromString(request.getTenantId());
            
            documentRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId))
                .ifPresentOrElse(d -> {
                    documentRepository.delete(d);
                    responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Document not found").asRuntimeException()));
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void listDocuments(ListDocumentsRequest request, StreamObserver<ListDocumentsResponse> responseObserver) {
        try {
            UUID tenantId = UUID.fromString(request.getTenantId());
            int page = request.getPage() > 0 ? request.getPage() - 1 : 0;
            int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 20;
            
            List<Document> documents;
            long total;
            
            if (request.getStatusesList().isEmpty()) {
                documents = documentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
                total = documents.size();
                documents = documents.stream().skip((long) page * pageSize).limit(pageSize).collect(Collectors.toList());
            } else {
                List<DocumentStatus> statuses = request.getStatusesList().stream()
                    .map(this::convertStatus)
                    .collect(Collectors.toList());
                documents = documentRepository.findByTenantIdAndStatusIn(tenantId, statuses);
                total = documents.size();
                documents = documents.stream().skip((long) page * pageSize).limit(pageSize).collect(Collectors.toList());
            }
            
            ListDocumentsResponse response = ListDocumentsResponse.newBuilder()
                .addAllDocuments(documents.stream().map(this::toProto).collect(Collectors.toList()))
                .setTotal((int) total)
                .setPage(page + 1)
                .setPageSize(pageSize)
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getDocumentPages(GetDocumentPagesRequest request, StreamObserver<GetDocumentPagesResponse> responseObserver) {
        try {
            UUID documentId = UUID.fromString(request.getDocumentId());
            UUID tenantId = UUID.fromString(request.getTenantId());
            
            documentRepository.findById(documentId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .ifPresentOrElse(d -> {
                    List<DocumentPage> pages = d.getPages();
                    GetDocumentPagesResponse response = GetDocumentPagesResponse.newBuilder()
                        .addAllPages(pages.stream().map(this::toProtoPage).collect(Collectors.toList()))
                        .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, () -> responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Document not found").asRuntimeException()));
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private DocumentProto toProto(Document doc) {
        DocumentProto.Builder builder = DocumentProto.newBuilder()
            .setId(doc.getId().toString())
            .setTenantId(doc.getTenantId().toString())
            .setDocumentTypeId(doc.getDocumentTypeId().toString())
            .setOriginalFilename(doc.getOriginalFilename())
            .setStoragePath(doc.getStoragePath())
            .setMimeType(doc.getMimeType())
            .setSizeBytes(doc.getSizeBytes())
            .setStatus(convertStatus(doc.getStatus()))
            .setCreatedAt(toTimestamp(doc.getCreatedAt()))
            .setUpdatedAt(toTimestamp(doc.getUpdatedAt()));
        
        if (doc.getProcessedAt() != null) {
            builder.setProcessedAt(toTimestamp(doc.getProcessedAt()));
        }
        
        if (doc.getMetadata() != null) {
            builder.putAllMetadata(fromJson(doc.getMetadata()));
        }
        
        for (DocumentPage page : doc.getPages()) {
            builder.addPages(toProtoPage(page));
        }
        
        return builder.build();
    }

    private DocumentPageProto toProtoPage(DocumentPage page) {
        DocumentPageProto.Builder builder = DocumentPageProto.newBuilder()
            .setPageNumber(page.getPageNumber())
            .setStoragePath(page.getStoragePath())
            .setWidthPx(page.getWidthPx() != null ? page.getWidthPx() : 0)
            .setHeightPx(page.getHeightPx() != null ? page.getHeightPx() : 0);
        
        if (page.getElements() != null) {
            // Parse JSON and add elements
            // Simplified for now
        }
        
        return builder.build();
    }

    private DocumentStatus convertStatus(DocumentStatusProto proto) {
        return switch (proto) {
            case DOCUMENT_STATUS_UPLOADED -> DocumentStatus.UPLOADED;
            case DOCUMENT_STATUS_QUEUED -> DocumentStatus.QUEUED;
            case DOCUMENT_STATUS_PROCESSING -> DocumentStatus.PROCESSING;
            case DOCUMENT_STATUS_REVIEW_REQUIRED -> DocumentStatus.REVIEW_REQUIRED;
            case DOCUMENT_STATUS_APPROVED -> DocumentStatus.APPROVED;
            case DOCUMENT_STATUS_REJECTED -> DocumentStatus.REJECTED;
            case DOCUMENT_STATUS_FAILED -> DocumentStatus.FAILED;
            case DOCUMENT_STATUS_ARCHIVED -> DocumentStatus.ARCHIVED;
            default -> DocumentStatus.UPLOADED;
        };
    }

    private DocumentStatusProto convertStatus(DocumentStatus status) {
        return switch (status) {
            case UPLOADED -> DocumentStatusProto.DOCUMENT_STATUS_UPLOADED;
            case QUEUED -> DocumentStatusProto.DOCUMENT_STATUS_QUEUED;
            case PROCESSING -> DocumentStatusProto.DOCUMENT_STATUS_PROCESSING;
            case REVIEW_REQUIRED -> DocumentStatusProto.DOCUMENT_STATUS_REVIEW_REQUIRED;
            case APPROVED -> DocumentStatusProto.DOCUMENT_STATUS_APPROVED;
            case REJECTED -> DocumentStatusProto.DOCUMENT_STATUS_REJECTED;
            case FAILED -> DocumentStatusProto.DOCUMENT_STATUS_FAILED;
            case ARCHIVED -> DocumentStatusProto.DOCUMENT_STATUS_ARCHIVED;
        };
    }

    private com.google.protobuf.Timestamp toTimestamp(Instant instant) {
        return com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    private String toJson(java.util.Map<String, String> map) {
        // Simplified JSON conversion
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private java.util.Map<String, String> fromJson(String json) {
        // Simplified - in production use Jackson
        return new java.util.HashMap<>();
    }
}