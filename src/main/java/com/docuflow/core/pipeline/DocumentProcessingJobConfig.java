package com.docuflow.core.pipeline;

import com.docuflow.core.pipeline.step.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class DocumentProcessingJobConfig {

    private final ClassificationStepConfig classificationStepConfig;
    private final OcrStepConfig ocrStepConfig;
    private final ExtractionStepConfig extractionStepConfig;
    private final ValidationStepConfig validationStepConfig;
    private final ExportStepConfig exportStepConfig;

    public DocumentProcessingJobConfig(ClassificationStepConfig classificationStepConfig,
                                        OcrStepConfig ocrStepConfig,
                                        ExtractionStepConfig extractionStepConfig,
                                        ValidationStepConfig validationStepConfig,
                                        ExportStepConfig exportStepConfig) {
        this.classificationStepConfig = classificationStepConfig;
        this.ocrStepConfig = ocrStepConfig;
        this.extractionStepConfig = extractionStepConfig;
        this.validationStepConfig = validationStepConfig;
        this.exportStepConfig = exportStepConfig;
    }

    @Bean
    public Job documentProcessingJob(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      @Qualifier("classificationStep") Step classificationStep,
                                      @Qualifier("ocrStep") Step ocrStep,
                                      @Qualifier("extractionStep") Step extractionStep,
                                      @Qualifier("validationStep") Step validationStep,
                                      @Qualifier("exportStep") Step exportStep) {
        return new JobBuilder("documentProcessingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(classificationStep)
            .next(ocrStep)
            .next(extractionStep)
            .next(validationStep)
            .next(exportStep)
            .build();
    }
}