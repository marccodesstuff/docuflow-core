package com.docuflow.core.pipeline.step;

import com.docuflow.core.entity.*;
import com.docuflow.core.repository.*;
import com.docuflow.core.service.OCRService;
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
public class OcrStepConfig {

    private final DocumentRepository documentRepository;
    private final ProcessingJobRepository jobRepository;
    private final OCRService ocrService;

    public OcrStepConfig(DocumentRepository documentRepository,
                          ProcessingJobRepository jobRepository,
                          OCRService ocrService) {
        this.documentRepository = documentRepository;
        this.jobRepository = jobRepository;
        this.ocrService = ocrService;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Document> ocrReader(@Value("#{jobParameters['tenantId']}") UUID tenantId) {
        return new JpaPagingItemReaderBuilder<Document>()
            .name("ocrReader")
            .entityManagerFactory(documentRepository.getEntityManager().getEntityManagerFactory())
            .queryString("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = :status")
            .parameterValues(Map.of(
                "tenantId", tenantId,
                "status", DocumentStatus.QUEUED
            ))
            .pageSize(5)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Document, Document> ocrProcessor() {
        return document -> {
            try {
                var pages = ocrService.processDocument(document.getStoragePath(), document.getMimeType());
                
                // Create DocumentPage entities
                List<DocumentPage> pageEntities = new java.util.ArrayList<>();
                for (int i = 0; i < pages.size(); i++) {
                    var page = pages.get(i);
                    DocumentPage pageEntity = new DocumentPage();
                    pageEntity.setDocument(document);
                    pageEntity.setPageNumber(i + 1);
                    pageEntity.setStoragePath(page.getImagePath());
                    pageEntity.setWidthPx(page.getWidth());
                    pageEntity.setHeightPx(page.getHeight());
                    pageEntity.setElements(page.getElementsJson());
                    pageEntities.add(pageEntity);
                }
                
                document.setPages(pageEntities);
                document.setStatus(DocumentStatus.PROCESSING);
                return document;
            } catch (Exception e) {
                document.setStatus(DocumentStatus.FAILED);
                throw new RuntimeException("OCR failed for document " + document.getId(), e);
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<Document> ocrWriter() {
        return documents -> {
            for (Document doc : documents) {
                documentRepository.save(doc);
            }
        };
    }

    @Bean
    public Step ocrStep(PlatformTransactionManager transactionManager,
                         JpaPagingItemReader<Document> reader,
                         ItemProcessor<Document, Document> processor,
                         ItemWriter<Document> writer) {
        return new StepBuilder("ocrStep", stepBuilderFactory -> stepBuilderFactory.getJobRepository())
            .<Document, Document>chunk(5, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(3)
            .skip(Exception.class)
            .build();
    }
}