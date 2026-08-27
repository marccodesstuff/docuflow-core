# DocuFlow Core

Spring Boot 3 + Spring Batch document processing pipeline. The domain core of the DocuFlow platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      DocuFlow Core                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │Classify │ │   OCR   │ │ Extract │ │ Validate│ │ Export │ │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬───┘ │
│       │           │           │           │           │      │
│       ▼           ▼           ▼           ▼           ▼      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                    Spring Batch Job                      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
   ┌──────────┐         ┌──────────┐         ┌──────────┐
   │PostgreSQL│         │  Kafka   │         │  MinIO   │
   └──────────┘         └──────────┘         └──────────┘
```

## Pipeline Steps

| Step | Description | Technology |
|------|-------------|------------|
| **Classify** | Document type classification | LayoutLMv3 via gRPC to ML sidecar |
| **OCR** | Text extraction from PDFs/images | Tesseract + PDFBox |
| **Extract** | Key-value field extraction | Donut via gRPC to ML sidecar |
| **Validate** | Regex, cross-field, confidence thresholds | Custom validation engine |
| **Export** | JSON, CSV, XML, XLSX, PDF | Jackson, Apache POI, iText |

## Tech Stack

- **Java 21**, Spring Boot 3.3, Spring Batch
- **PostgreSQL** with Liquibase migrations
- **Kafka** for event streaming
- **MinIO/S3** for document storage
- **gRPC** for inter-service communication
- **Prometheus** metrics, **Grafana** dashboards

## Quick Start

### Prerequisites

- JDK 21+
- Docker Compose (for dependencies)

### Local Development

```bash
# Start dependencies (PostgreSQL, Kafka, MinIO, etc.)
cd ../docuflow-infra/docker
docker compose up -d postgres kafka minio redis

# Build and run
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Run Tests

```bash
./gradlew test
```

### Build Docker Image

```bash
docker build -t docuflow-core:latest .
```

## Configuration

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/docuflow_core` | PostgreSQL connection |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO endpoint |
| `GRPC_PORT` | `9090` | gRPC server port |
| `ML_SIDECAR_URL` | `http://localhost:50051` | ML inference sidecar |

## API (gRPC)

Defined in `docuflow-shared/proto/docuflow/v1/document.proto`:

- `DocumentService` - CRUD for documents
- `ProcessingJobService` - Job orchestration
- `ExtractionService` - Extraction, validation, export
- `WebhookService` - Webhook registration/delivery
- `MLInferenceService` - Classification, extraction, detection

## Database Schema

Managed by Liquibase in `src/main/resources/db/changelog/`:

- `documents` - Document metadata
- `document_pages` - Rendered page images
- `document_types` - Configurable document schemas
- `field_definitions` - Field definitions with validation
- `processing_jobs` - Pipeline job tracking
- `job_steps` - Individual step execution
- `extraction_results` - Extracted fields & tables
- `webhook_registrations` - Webhook endpoints
- `webhook_deliveries` - Delivery audit log

## Monitoring

- **Health**: `GET /actuator/health`
- **Metrics**: `GET /actuator/prometheus`
- **Grafana Dashboard**: DocuFlow Core Overview

## License

MIT