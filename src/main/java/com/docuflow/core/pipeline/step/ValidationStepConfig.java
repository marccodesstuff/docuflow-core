package com.docuflow.core.pipeline.step;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.*;
import com.docuflow.core.service.ValidationService;
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
public class ValidationStepConfig {

    private final DocumentRepository documentRepository;
    private final ExtractionResultRepository extractionResultRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final ValidationService validationService;

    public ValidationStepConfig(DocumentRepository documentRepository,
                                 ExtractionResultRepository extractionResultRepository,
                                 DocumentTypeRepository documentTypeRepository,
                                 ValidationService validationService) {
        this.documentRepository = documentRepository;
        this.extractionResultRepository = extractionResultRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.validationService = validationService;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<ExtractionResult> validationReader(@Value("#{jobParameters['tenantId']}") UUID tenantId) {
        return new JpaPagingItemReaderBuilder<ExtractionResult>()
            .name("validationReader")
            .entityManagerFactory(extractionResultRepository.getEntityManager().getEntityManagerFactory())
            .queryString("SELECT er FROM ExtractionResult er JOIN Document d ON er.documentId = d.id WHERE d.tenantId = :tenantId AND d.status = :status")
            .parameterValues(Map.of(
                "tenantId", tenantId,
                "status", DocumentStatus.REVIEW_REQUIRED
            ))
            .pageSize(10)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<ExtractionResult, ExtractionResult> validationProcessor() {
        return extractionResult -> {
            try {
                Document document = documentRepository.findById(extractionResult.getDocumentId())
                    .orElseThrow(() -> new IllegalStateException("Document not found"));
                
                DocumentType docType = documentTypeRepository.findById(document.getDocumentTypeId())
                    .orElseThrow(() -> new IllegalStateException("Document type not found"));
                
                ValidationService.ValidationResult validation = validationService.validate(
                    extractionResult, docType);
                
                if (validation.isValid() && validation.getAutoApprove()) {
                    extractionResult.setStatus(ExtractionStatus.SUCCESS);
                    document.setStatus(DocumentStatus.APPROVED);
                } else if (validation.isValid()) {
                    extractionResult.setStatus(ExtractionStatus.SUCCESS);
                    document.setStatus(DocumentStatus.REVIEW_REQUIRED);
                } else {
                    extractionResult.setStatus(ExtractionStatus.REVIEW_NEEDED);
                    document.setStatus(DocumentStatus.REVIEW_REQUIRED);
                }
                
                documentRepository.save(document);
                return extractionResult;
            } catch (Exception e) {
                throw new RuntimeException("Validation failed for extraction " + extractionResult.getId(), e);
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<ExtractionResult> validationWriter() {
        return results -> {
            for (ExtractionResult result : results) {
                extractionResultRepository.save(result);
            }
        };
    }

    @Bean
    public Step validationStep(PlatformTransactionManager transactionManager,
                                JpaPagingItemReader<ExtractionResult> reader,
                                ItemProcessor<ExtractionResult, ExtractionResult> processor,
                                ItemWriter<ExtractionResult> writer) {
        return new StepBuilder("validationStep", stepBuilderFactory -> stepBuilderFactory.getJobRepository())
            .<ExtractionResult, ExtractionResult>chunk(10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(5)
            .skip(Exception.class)
            .build();
    }
}