package com.docuflow.core.service;

import com.docuflow.core.entity.Document;
import com.docuflow.shared.proto.*;
import io.grpc.ManagedChannel;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
public class StorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public StorageService(MinioClient minioClient, @Value("${storage.bucket:docuflow}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public byte[] download(String objectKey) {
        try (InputStream stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download object: " + objectKey, e);
        }
    }

    public void upload(String objectKey, byte[] data, String contentType) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(stream, data.length, -1)
                .contentType(contentType)
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload object: " + objectKey, e);
        }
    }

    public String getPresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                io.minio.GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expiryMinutes * 60)
                    .method(io.minio.http.Method.GET)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + objectKey, e);
        }
    }
}