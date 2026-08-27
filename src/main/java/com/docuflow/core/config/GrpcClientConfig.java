package com.docuflow.core.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Value("${docuflow.pipeline.extract.ml-sidecar.url:http://localhost:50051}")
    private String mlSidecarUrl;

    @Bean
    public ManagedChannel mlSidecarChannel() {
        // Parse host:port from URL
        String hostPort = mlSidecarUrl.replace("http://", "").replace("https://", "");
        String[] parts = hostPort.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 50051;
        
        return ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()  // Use TLS in production
            .build();
    }
}