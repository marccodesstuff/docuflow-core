package com.docuflow.core.pipeline.step;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.*;
import com.docuflow.core.service.ExtractionService;
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

import java.util.List;
import java.util.Map;

@Configuration
public class ExtractionStepConfig {

    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ProcessingJobRepository jobRepository;
    private final ExtractionResultRepository extractionResultRepository;
    private final ExtractionService extractionService;

    public ExtractionStepConfig(DocumentRepository documentRepository,
                                 DocumentTypeRepository documentTypeRepository,
                                 ProcessingJobRepository jobRepository,
                                 ExtractionResultRepository extractionResultRepository,
                                 ExtractionService extractionService) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.jobRepository = jobRepository;
        this.extractionResultRepository = extractionResultRepository;
        this.extractionService = extractionService;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Document> extractionReader(@Value("#{jobParameters['tenantId']}") UUID tenantId) {
        return new JpaPagingItemReaderBuilder<Document>()
            .name("extractionReader")
            .entityManagerFactory(documentRepository.getEntityManager().getEntityManagerFactory())
            .queryString("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status")
            .parameterValues(Map.of(
                "tenantId", tenantId,
                "status", DocumentStatus.PROCESSING
            ))
            .pageSize(5)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Document, ExtractionResult> extractionProcessor() {
        return document -> {
            try {
                DocumentType docType = documentTypeRepository.findById(document.getDocumentTypeId())
                    .orElseThrow(() -> new IllegalStateException("Document type not found"));
                
                var result = extractionService.extractDocument(
                    document.getId().toString(),
                    document.getStoragePath(),
                    document.getMimeType(),
                    document.getDocumentTypeId().toString(),
                    docType.getFields()
                );
                
                ExtractionResult extractionResult = new ExtractionResult();
                extractionResult.setJobId(UUID.randomUUID()); // Will be set by job
                extractionResult.setDocumentId(document.getId());
                extractionResult.setStatus(result.getStatus());
                extractionResult.setFields(result.getFields());
                extractionResult.setTables(result.getTables());
                extractionResult.setOverallConfidence(result.getOverallConfidence());
                extractionResult.setModelVersion(result.getModelVersion());
                extractionResult.setIssues(result.getIssuesJson());
                
                // Update document status based on extraction result
                if (result.getStatus() == ExtractionStatus.REVIEW_NEEDED) {
                    document.setStatus(DocumentStatus.REVIEW_REQUIRED);
                } else if (result.getStatus() == ExtractionStatus.FAILED) {
                    document.setStatus(DocumentStatus.FAILED);
                } else {
                    document.setStatus(DocumentStatus.REVIEW_REQUIRED); // Default to review
                }
                documentRepository.save(document);
                
                return extractionResult;
            } catch (Exception e) {
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.save(document);
                throw new RuntimeException("Extraction failed for document " + document.getId(), e);
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<ExtractionResult> extractionWriter() {
        return results -> {
            for (ExtractionResult result : results) {
                extractionResultRepository.save(result);
            }
        };
    }

    @Bean
    public Step extractionStep(PlatformTransactionManager transactionManager,
                                JpaPagingItemReader<Document> reader,
                                ItemProcessor<Document, ExtractionResult> processor,
                                ItemWriter<ExtractionResult> writer) {
        return new StepBuilder("extractionStep", stepBuilderFactory -> stepBuilderFactory.getJobRepository())
            .<Document, ExtractionResult>chunk(5, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(3)
            .skip(Exception.class)
            .build();
    }
}