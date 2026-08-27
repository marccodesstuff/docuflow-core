package com.docuflow.core.pipeline.step;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.*;
import com.docuflow.core.service.ExportService;
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
public class ExportStepConfig {

    private final DocumentRepository documentRepository;
    private final ExtractionResultRepository extractionResultRepository;
    private final ExportService exportService;

    public ExportStepConfig(DocumentRepository documentRepository,
                             ExtractionResultRepository extractionResultRepository,
                             ExportService exportService) {
        this.documentRepository = documentRepository;
        this.extractionResultRepository = extractionResultRepository;
        this.exportService = exportService;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<ExtractionResult> exportReader(@Value("#{jobParameters['tenantId']}") UUID tenantId) {
        return new JpaPagingItemReaderBuilder<ExtractionResult>()
            .name("exportReader")
            .entityManagerFactory(extractionResultRepository.getEntityManager().getEntityManagerFactory())
            .queryString("SELECT er FROM ExtractionResult er JOIN Document d ON er.documentId = d.id WHERE d.tenantId = :tenantId AND d.status IN :statuses")
            .parameterValues(Map.of(
                "tenantId", tenantId,
                "statuses", List.of(DocumentStatus.APPROVED, DocumentStatus.REVIEW_REQUIRED)
            ))
            .pageSize(20)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<ExtractionResult, ExportService.ExportResult> exportProcessor() {
        return extractionResult -> {
            try {
                Document document = documentRepository.findById(extractionResult.getDocumentId())
                    .orElseThrow(() -> new IllegalStateException("Document not found"));
                
                return exportService.export(extractionResult, document, ExportFormat.JSON);
            } catch (Exception e) {
                throw new RuntimeException("Export failed for extraction " + extractionResult.getId(), e);
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<ExportService.ExportResult> exportWriter() {
        return results -> {
            // Results are already saved by exportService
            for (ExportService.ExportResult result : results) {
                // Log success
            }
        };
    }

    @Bean
    public Step exportStep(PlatformTransactionManager transactionManager,
                            JpaPagingItemReader<ExtractionResult> reader,
                            ItemProcessor<ExtractionResult, ExportService.ExportResult> processor,
                            ItemWriter<ExportService.ExportResult> writer) {
        return new StepBuilder("exportStep", stepBuilderFactory -> stepBuilderFactory.getJobRepository())
            .<ExtractionResult, ExportService.ExportResult>chunk(20, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(10)
            .skip(Exception.class)
            .build();
    }

    public enum ExportFormat { JSON, CSV, XML, XLSX, PDF }
}