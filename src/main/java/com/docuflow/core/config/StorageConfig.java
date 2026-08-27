package com.docuflow.core.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class StorageConfig {

    @Value("${storage.type:minio}")
    private String storageType;

    @Value("${storage.minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${storage.minio.access-key:minioadmin}")
    private String minioAccessKey;

    @Value("${storage.minio.secret-key:minioadmin}")
    private String minioSecretKey;

    @Value("${storage.minio.bucket:docuflow}")
    private String minioBucket;

    @Value("${storage.minio.region:us-east-1}")
    private String minioRegion;

    @Value("${storage.s3.region:us-east-1}")
    private String s3Region;

    @Value("${storage.s3.bucket:docuflow}")
    private String s3Bucket;

    @Value("${storage.s3.credentials.access-key:}")
    private String s3AccessKey;

    @Value("${storage.s3.credentials.secret-key:}")
    private String s3SecretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(minioEndpoint)
            .credentials(minioAccessKey, minioSecretKey)
            .build();
    }

    @Bean
    public S3Client s3Client() {
        if ("s3".equals(storageType)) {
            return S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                .build();
        }
        // MinIO uses S3-compatible API
        return S3Client.builder()
            .region(Region.of(minioRegion))
            .endpointOverride(java.net.URI.create(minioEndpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(minioAccessKey, minioSecretKey)))
            .forcePathStyle(true)
            .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        if ("s3".equals(storageType)) {
            return S3Presigner.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                .build();
        }
        return S3Presigner.builder()
            .region(Region.of(minioRegion))
            .endpointOverride(java.net.URI.create(minioEndpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(minioAccessKey, minioSecretKey)))
            .build();
    }

    @Bean
    public String storageBucket() {
        return "s3".equals(storageType) ? s3Bucket : minioBucket;
    }
}