# FINT Flyt File Service

Reactive Spring Boot service that accepts file uploads from internal FINT Flyt clients, stores binary payloads in Azure
Blob Storage, and makes them retrievable over HTTP. The service caches hot entries, enforces validation, and reacts to
Kafka “instance-deleted” events to remove associated files automatically.

## Highlights

- **Reactive HTTP API** — non-blocking Spring WebFlux controller for posting and fetching files.
- **Azure Blob integration** — uploads file contents with metadata/tags and cleans up stale blobs on a daily schedule.
- **Kafka-driven cleanup** — consumes instance lifecycle events to delete files when upstream processes finish.
- **In-memory caching** — wraps file lookups in a `FintCache` layer to avoid redundant blob downloads.
- **Observability** — readiness/health endpoints and Prometheus metrics enabled via Spring Boot Actuator.

## Architecture Overview

| Component                              | Responsibility                                                                                         |
|----------------------------------------|--------------------------------------------------------------------------------------------------------|
| `FileController`                       | Validates and handles `POST /internal/api/filer` uploads and `GET /internal/api/filer/{id}` downloads. |
| `FileService`                          | Orchestrates cache lookups, repository calls, and basic validation.                                    |
| `FileRepository`                       | Delegates persistence actions to `AzureBlobAdapter`.                                                   |
| `AzureBlobAdapter`                     | Talks to Azure Blob Storage using the async SDK; manages metadata, tags, and retries.                  |
| `InstanceDeletedConsumerConfiguration` | Builds a Kafka listener that pulls file IDs from instance events and triggers deletion.                |
| `FileCleanupService`                   | Scheduled job that purges blobs older than a configured age.                                           |

## HTTP API

Base path: `/internal/api/filer`

| Method | Path        | Description                                 | Request body                                                                   | Response                                                |
|--------|-------------|---------------------------------------------|--------------------------------------------------------------------------------|---------------------------------------------------------|
| `POST` | `/`         | Store a file and return its generated UUID. | JSON matching the `File` schema below; the `contents` field is base64-encoded. | `201 Created` with UUID in the body.                    |
| `GET`  | `/{fileId}` | Retrieve a previously stored file.          | –                                                                              | `200 OK` with the `File` payload, `404` when not found. |

`File` payload fields:

```json
{
"name": "example.pdf",          // required
"sourceApplicationId": 123,     // required
"sourceApplicationInstanceId": "instance-1", // required
"type": "application/pdf",      // optional MediaType
"encoding": "base64",           // optional, echoed back from metadata
"contents": "JVBERi0xLjcKJYGBgY..." // required, base64 for byte[]
}
```

Errors return standard Spring WebFlux responses; validation failures yield 400 Bad Request.

## Kafka Integration

- Listens to instance-deleted events through InstanceFlowListenerFactoryService.
- Expects InstanceFlowHeaders to carry fileIds; each ID triggers FileService.delete.
- Kafka configuration is sourced from application-flyt-kafka.yaml, inheriting the shared fint topic settings.

## Scheduled Tasks

FileCleanupService.cleanup() runs every 24 hours (initial delay 30 seconds) and deletes blobs older than fint.flyt.file-service.time-to-keep-azure-blobs-in-days (default 180).

## Configuration

The service uses layered Spring profiles: flyt-kafka, flyt-logging, and flyt-resource-server.

Key properties:

| Property | Description |
| --- | --- |
| fint.azure.storage-account.connection-string | Connection string for the Azure Storage account. |
| fint.azure.storage.container-blob.name | Target container that stores files. |
| fint.kafka.topic.orgId | Overridden per overlay to scope Kafka ACLs. |
| fint.application-id | Defaults to fint-flyt-file-service. |
| fint.flyt.file-service.time-to-keep-azure-blobs-in-days | Retention window for scheduled cleanup. |
| spring.security.oauth2.resourceserver.jwt.issuer-uri | Issuer for token validation. |
| spring.codec.max-in-memory-size | Raised to 100 MB to support large uploads. |

Secrets referenced in the base kustomize manifest must provide Azure credentials and OAuth client information.

## Running Locally

Prerequisites:

- Java 21+
- Gradle (wrapper bundled)
- Kafka broker and Azure Blob emulator/real account

Useful commands:
```shell
./gradlew clean build        # compile and run tests
./gradlew bootRun            # launch the service with local profiles
./gradlew test               # unit tests
```

For local testing set SPRING_PROFILES_ACTIVE=local-staging to use the dev-focused configuration in application-local-staging.yaml. Provide a storage connection string (UseDevelopmentStorage=true for Azurite) and Kafka bootstrap server (localhost:9092 by default).

## Deployment

Kustomize structure:

- kustomize/base/ contains common resources for the FINT Flyt platform (Application, OAuth client, Azure blob container).
- kustomize/overlays/<org>/<env>/ holds organization-specific patches (namespace, labels, Kafka topics, and optional env overrides).

Templates live under kustomize/templates/:

- overlay.yaml.tpl — standard overlay

Regenerate overlays after editing templates:
```shell
bin/render-overlays.sh
```

The script maps each overlay to the right template, applies overrides, and writes kustomization.yaml files in place.

## Security

- Exposes OAuth2 resource server with JWT validation (issuer at https://idp.felleskomponent.no).
- Internal API paths restricted via fint.flyt.resource-server.security.api.internal-client.

## Observability & Operations

- Readiness probe at /actuator/health.
- Prometheus metrics available via /actuator/prometheus.
- Logs use standard Spring configuration and Reactor context logging for errors.

## Development Tips

- FileService caches downloads; consider invalidating the cache when modifying testing scenarios.
- AzureBlobAdapter relies on metadata and tags—mock these in tests or integration fixtures if you assert on derived fields.

## Contributing

1. Create a topic branch.
2. Run ./gradlew test before raising a PR.
3. If you touch kustomize overlays, regenerate them via bin/render-overlays.sh and commit the changes.
4. Add/adjust unit tests for new behavior.

———

FINT Flyt File Service is maintained by the FINT Flyt team. Reach out on the internal Slack channel or create an issue in this repository for questions or enhancements.