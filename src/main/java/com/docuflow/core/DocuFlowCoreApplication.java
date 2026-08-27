package com.docuflow.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.docuflow.core")
@EntityScan("com.docuflow.core.entity")
@EnableJpaRepositories("com.docuflow.core.repository")
@EnableAsync
@EnableScheduling
public class DocuFlowCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocuFlowCoreApplication.class, args);
    }
}