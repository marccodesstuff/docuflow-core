package com.docuflow.core.pipeline.step;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.*;
import com.docuflow.core.service.DocumentService;
import com.docuflow.core.service.MLInferenceClient;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class ClassificationStepConfig {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ProcessingJobRepository jobRepository;
    private final MLInferenceClient mlInferenceClient;
    private final DocumentService documentService;

    public ClassificationStepConfig(DocumentRepository documentRepository,
                                     DocumentTypeRepository documentTypeRepository,
                                     ProcessingJobRepository jobRepository,
                                     MLInferenceClient mlInferenceClient,
                                     DocumentService documentService) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.jobRepository = jobRepository;
        this.mlInferenceClient = mlInferenceClient;
        this.documentService = documentService;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Document> classificationReader(@Value("#{jobParameters['tenantId']}") UUID tenantId,
                                                               @Value("#{jobParameters['documentTypeIds']}") List<UUID> documentTypeIds) {
        return new JpaPagingItemReaderBuilder<Document>()
            .name("classificationReader")
            .entityManagerFactory(documentRepository.getEntityManager().getEntityManagerFactory())
            .queryString("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.documentTypeId IN :documentTypeIds AND d.status = :status")
            .parameterValues(Map.of(
                "tenantId", tenantId,
                "documentTypeIds", documentTypeIds,
                "status", DocumentStatus.UPLOADED
            ))
            .pageSize(10)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Document, Document> classificationProcessor() {
        return document -> {
            try {
                // Call ML sidecar for classification
                var result = mlInferenceClient.classifyDocument(
                    document.getId().toString(),
                    document.getStoragePath(),
                    document.getMimeType(),
                    documentTypeRepository.findByTenantId(document.getTenantId())
                        .stream()
                        .map(DocumentType::getId)
                        .map(UUID::toString)
                        .toList()
                );
                
                if (result.getConfidence() > 0.8) {
                    document.setDocumentTypeId(UUID.fromString(result.getPredictedTypeId()));
                    document.setStatus(DocumentStatus.QUEUED);
                } else {
                    document.setStatus(DocumentStatus.REVIEW_REQUIRED);
                }
                return document;
            } catch (Exception e) {
                document.setStatus(DocumentStatus.FAILED);
                throw new RuntimeException("Classification failed for document " + document.getId(), e);
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<Document> classificationWriter() {
        return documents -> {
            for (Document doc : documents) {
                documentRepository.save(doc);
            }
        };
    }

    @Bean
    public Step classificationStep(PlatformTransactionManager transactionManager,
                                    JpaPagingItemReader<Document> reader,
                                    ItemProcessor<Document, Document> processor,
                                    ItemWriter<Document> writer) {
        return new StepBuilder("classificationStep", stepBuilderFactory -> stepBuilderFactory
            .getJobRepository())
            .<Document, Document>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(5)
            .skip(Exception.class)
            .build();
    }
}