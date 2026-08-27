package com.docuflow.core.service;

import com.docuflow.shared.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class MLInferenceClient {

    private final ManagedChannel channel;
    private final MLInferenceServiceGrpc.MLInferenceServiceBlockingStub blockingStub;
    private final MLInferenceServiceGrpc.MLInferenceServiceStub asyncStub;

    public MLInferenceClient(ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = MLInferenceServiceGrpc.newBlockingStub(channel);
        this.asyncStub = MLInferenceServiceGrpc.newStub(channel);
    }

    public ClassifyDocumentResponse classifyDocument(String documentId, String storagePath, 
                                                      String mimeType, List<String> candidateTypes) {
        ClassifyDocumentRequest request = ClassifyDocumentRequest.newBuilder()
            .setDocumentId(documentId)
            .setStoragePath(storagePath)
            .setMimeType(mimeType)
            .addAllCandidateTypes(candidateTypes)
            .build();
        
        return blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).classifyDocument(request);
    }

    public CompletableFuture<ExtractFieldsResponse> extractFieldsAsync(String documentId, String storagePath,
                                                                        String mimeType, String documentTypeId,
                                                                        java.util.Map<String, FieldDefinition> fieldSchema) {
        ExtractFieldsRequest.Builder builder = ExtractFieldsRequest.newBuilder()
            .setDocumentId(documentId)
            .setStoragePath(storagePath)
            .setMimeType(mimeType)
            .setDocumentTypeId(documentTypeId);
        
        // Convert field schema
        for (var entry : fieldSchema.entrySet()) {
            builder.putFieldSchema(entry.getKey(), convertFieldDefinition(entry.getValue()));
        }
        
        ExtractFieldsRequest request = builder.build();
        
        CompletableFuture<ExtractFieldsResponse> future = new CompletableFuture<>();
        asyncStub.extractFields(request, new StreamObserver<>() {
            @Override
            public void onNext(ExtractFieldsResponse value) {
                future.complete(value);
            }
            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }
            @Override
            public void onCompleted() {}
        });
        
        return future;
    }

    public ExtractFieldsResponse extractFields(String documentId, String storagePath,
                                               String mimeType, String documentTypeId,
                                               java.util.Map<String, FieldDefinition> fieldSchema) {
        try {
            return extractFieldsAsync(documentId, storagePath, mimeType, documentTypeId, fieldSchema)
                .get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Field extraction failed", e);
        }
    }

    public DetectTablesResponse detectTables(String documentId, String storagePath, String mimeType, int maxTables) {
        DetectTablesRequest request = DetectTablesRequest.newBuilder()
            .setDocumentId(documentId)
            .setStoragePath(storagePath)
            .setMimeType(mimeType)
            .setMaxTables(maxTables)
            .build();
        
        return blockingStub.withDeadlineAfter(60, TimeUnit.SECONDS).detectTables(request);
    }

    public DetectElementsResponse detectElements(String documentId, String storagePath, 
                                                  String mimeType, List<String> elementTypes) {
        DetectElementsRequest request = DetectElementsRequest.newBuilder()
            .setDocumentId(documentId)
            .setStoragePath(storagePath)
            .setMimeType(mimeType)
            .addAllElementTypes(elementTypes)
            .build();
        
        return blockingStub.withDeadlineAfter(60, TimeUnit.SECONDS).detectElements(request);
    }

    public HealthCheckResponse healthCheck() {
        return blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).healthCheck(HealthCheckRequest.getDefaultInstance());
    }

    private FieldDefinition convertFieldDefinition(com.docuflow.core.entity.FieldDefinition fd) {
        return FieldDefinition.newBuilder()
            .setKey(fd.getKey())
            .setLabel(fd.getLabel())
            .setType(convertFieldType(fd.getType()))
            .setRequired(fd.getRequired())
            .setDescription(fd.getDescription() != null ? fd.getDescription() : "")
            .setRegexPattern(fd.getRegexPattern() != null ? fd.getRegexPattern() : "")
            .addAllEnumValues(fd.getEnumValues() != null ? java.util.Arrays.asList(fd.getEnumValues().split(",")) : java.util.List.of())
            .build();
    }

    private FieldDefinition.FieldType convertFieldType(com.docuflow.core.entity.FieldType type) {
        return switch (type) {
            case STRING -> FieldDefinition.FieldType.FIELD_TYPE_STRING;
            case NUMBER -> FieldDefinition.FieldType.FIELD_TYPE_NUMBER;
            case BOOLEAN -> FieldDefinition.FieldType.FIELD_TYPE_BOOLEAN;
            case DATE -> FieldDefinition.FieldType.FIELD_TYPE_DATE;
            case DATETIME -> FieldDefinition.FieldType.FIELD_TYPE_DATETIME;
            case EMAIL -> FieldDefinition.FieldType.FIELD_TYPE_EMAIL;
            case PHONE -> FieldDefinition.FieldType.FIELD_TYPE_PHONE;
            case CURRENCY -> FieldDefinition.FieldType.FIELD_TYPE_CURRENCY;
            case TABLE -> FieldDefinition.FieldType.FIELD_TYPE_TABLE;
            case OBJECT -> FieldDefinition.FieldType.FIELD_TYPE_OBJECT;
            case ARRAY -> FieldDefinition.FieldType.FIELD_TYPE_ARRAY;
        };
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}