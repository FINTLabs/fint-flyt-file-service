# FINT Flyt File Service

Kotlin-based Spring Boot service for uploading and retrieving files in Flyt.
The service exposes an internal HTTP API, stores files in pluggable storage (Azure in production, in-memory locally), uses caching for fast lookups, and removes files through both scheduled jobs and Kafka events.

## Highlights

- Spring MVC (`spring-boot-starter-web`).
- Domain model in Kotlin.
- Global exception handling with `ProblemDetail`.
- JSON logging via Logback + logstash encoder.
- Local development without Azure using an in-memory storage adapter.

## Architecture

| Component | Responsibility |
| --- | --- |
| `FileController` | Endpoints for `POST` and `GET` of files. |
| `FileService` | Business flow, cache lookups, and repository fallback. |
| `FileRepository` | Storage operations through `BlobStorageAdapter`. |
| `BlobStorageAdapter` | Storage abstraction (port). |
| `AzureBlobAdapter` | Azure Blob implementation (`!local-staging`). |
| `InMemoryBlobAdapter` | Local fake storage (`local-staging`). |
| `GlobalExceptionHandler` | Standardized `ProblemDetail` error responses. |
| `FileCleanupService` | Daily cleanup of old files. |
| `InstanceDeletedConsumerConfiguration` | Kafka listener for deleting files on `instance-deleted` events. |

## Package Structure

- `no.novari.flyt.files.api`
- `no.novari.flyt.files.application`
- `no.novari.flyt.files.domain`
- `no.novari.flyt.files.infrastructure.storage`
- `no.novari.flyt.files.infrastructure.storage.azure`
- `no.novari.flyt.files.infrastructure.storage.inmemory`
- `no.novari.flyt.files.infrastructure.kafka`
- `no.novari.flyt.api.error`

## HTTP API

Base path: `/api/intern-klient/filer`

| Method | Path | Description | Response |
| --- | --- | --- | --- |
| `POST` | `/api/intern-klient/filer` | Stores a file and returns a generated UUID. | `201 Created` + UUID in the body |
| `GET` | `/api/intern-klient/filer/{fileId}` | Retrieves a file by UUID. | `200 OK` + `FilePayload` |

`FilePayload` (request/response):

```json
{
  "name": "example.pdf",
  "sourceApplicationId": 123,
  "sourceApplicationInstanceId": "instance-1",
  "type": "application/pdf",
  "encoding": "base64",
  "contents": "JVBERi0xLjcKJYGBgY..."
}
```

`contents` is base64 in JSON and is mapped to `ByteArray` internally.

## Error Handling

The API returns `application/problem+json` (`ProblemDetail`) without internal details in the response.

Typical statuses:

- `400 Bad Request`: validation errors, invalid JSON, invalid UUID format.
- `404 Not Found`: file does not exist.
- `405 Method Not Allowed`: unsupported HTTP method.
- `500 Internal Server Error`: unexpected server errors.

## Storage

### Production / non-local

`AzureBlobAdapter` is active when the profile is **not** `local-staging`:

- Reads:
  - `fint.azure.storage-account.connection-string`
  - `fint.azure.storage.container-blob.name`

### Local (`local-staging`)

`InMemoryBlobAdapter` is active in `local-staging`, so Azure credentials are not required locally.

## Security

The project uses `flyt-web-resource-server`.

Default profiles in `application.yaml`:

- `flyt-kafka`
- `flyt-logging`
- `flyt-web-resource-server`

Local auth/JWT bypass requires **two** conditions:

1. Profile `local-staging`
2. Property `novari.flyt.file-service.local-security.permit-all-enabled=true`

This is controlled in `LocalPermitAllSecurityConfiguration`.

The following is also set in `application-local-staging.yaml`:

- `novari.flyt.web-resource-server.security.api.internal.enabled=false`
- `novari.flyt.web-resource-server.security.api.internal-client.enabled=false`
- `novari.flyt.web-resource-server.security.api.external.enabled=false`

## Logging and Observability

- Logging: `kotlin-logging` (`io.github.oshai:kotlin-logging-jvm`)
- Log format: JSON via `logback.xml` + `logstash-logback-encoder`
- Actuator: health and metrics (`/actuator/health`, `/actuator/prometheus`)

## Configuration

| Property | Description |
| --- | --- |
| `fint.application-id` | App ID, default `fint-flyt-file-service`. |
| `novari.flyt.file-service.time-to-keep-files-in-days` | Retention window for cleanup (default 61). |
| `fint.azure.storage-account.connection-string` | Azure connection string (not required in `local-staging`). |
| `fint.azure.storage.container-blob.name` | Azure container name (not required in `local-staging`). |
| `novari.flyt.file-service.local-security.permit-all-enabled` | Local `permitAll` when `local-staging` is active. |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids` | Authorized internal client IDs. |

## Local Run

Prerequisites:

- Java 25+
- Gradle wrapper (`./gradlew`)
- Local Kafka on `localhost:9092` (for Kafka-related beans)

Commands:

```shell
./gradlew clean build
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local-staging'
```

With `local-staging`, the service uses in-memory storage.

## Example Manual Test

```shell
BASE_URL="http://localhost:8091"
API="$BASE_URL/api/intern-klient/filer"

curl -i -X POST "$API" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test.txt",
    "sourceApplicationId": 123,
    "sourceApplicationInstanceId": "instance-1",
    "contents": "SGVsbG8="
  }'
```
