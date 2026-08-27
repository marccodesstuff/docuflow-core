plugins {
    id("java")
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.google.protobuf") version "0.9.4"
    id("maven-publish")
}

group = "com.docuflow.core"
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://maven.pkg.github.com/docuflow/docuflow-shared") }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    
    // gRPC
    implementation("io.grpc:grpc-spring-boot-starter:3.0.0")
    implementation("io.grpc:grpc-protobuf:1.65.1")
    implementation("io.grpc:grpc-stub:1.65.1")
    
    // DocuFlow shared contracts
    implementation("com.docuflow.shared:docuflow-shared:1.0.0-SNAPSHOT")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")
    
    // Messaging
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.amqp:spring-rabbit")
    
    // Object storage
    implementation("io.minio:minio:8.5.10")
    implementation("software.amazon.awssdk:s3:2.25.26")
    
    // Document processing
    implementation("org.apache.pdfbox:pdfbox:3.0.2")
    implementation("org.apache.tika:tika-core:3.0.0")
    implementation("net.sourceforge.tess4j:tess4j:5.8.0")
    
    // Utilities
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.apache.commons:commons-lang3:3.15.0")
    implementation("com.google.guava:guava:33.2.1-jre")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.testcontainers:kafka:1.20.1")
    testImplementation("org.testcontainers:localstack:1.20.1")
    testImplementation("io.grpc:grpc-testing:1.65.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
        mavenBom("org.testcontainers:testcontainers-bom:1.20.1")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.65.1"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
            }
            task.builtins {
                id("java")
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation" << "-parameters"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    systemProperty("spring.profiles.active", "test")
}

springBoot {
    buildInfo()
}