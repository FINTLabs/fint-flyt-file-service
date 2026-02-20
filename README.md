# FINT Flyt File Service

Kotlin-basert Spring Boot-tjeneste for opplasting og uthenting av filer i Flyt.
Tjenesten eksponerer et internt HTTP-API, lagrer filer i pluggbar storage (Azure i prod, in-memory lokalt), bruker cache for raske oppslag, og rydder filer via både schedule og Kafka-eventer.

## Høydepunkter

- Spring MVC (`spring-boot-starter-web`).
- Domenemodell i Kotlin.
- Global exception handling med `ProblemDetail`.
- JSON-logging via Logback + logstash encoder.
- Lokal utvikling uten Azure med in-memory storage adapter.

## Arkitektur

| Komponent | Ansvar |
| --- | --- |
| `FileController` | Endepunkter for `POST` og `GET` av filer. |
| `FileService` | Forretningsflyt, cache-oppslag og fallback til repository. |
| `FileRepository` | Lagringsoperasjoner via `BlobStorageAdapter`. |
| `BlobStorageAdapter` | Lagringsabstraksjon (port). |
| `AzureBlobAdapter` | Azure Blob-implementasjon (`!local-staging`). |
| `InMemoryBlobAdapter` | Lokal fake-lagring (`local-staging`). |
| `GlobalExceptionHandler` | Standardiserte `ProblemDetail`-responser for feil. |
| `FileCleanupService` | Daglig opprydding av gamle filer. |
| `InstanceDeletedConsumerConfiguration` | Kafka-listener for sletting av filer på `instance-deleted` event. |

## Pakkestruktur

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

| Method | Path | Beskrivelse | Respons |
| --- | --- | --- | --- |
| `POST` | `/api/intern-klient/filer` | Lagrer fil og returnerer generert UUID. | `201 Created` + UUID i body |
| `GET` | `/api/intern-klient/filer/{fileId}` | Henter fil på UUID. | `200 OK` + `FilePayload` |

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

`contents` er base64 i JSON og mapes til `ByteArray` internt.

## Feilhåndtering

API-et returnerer `application/problem+json` (`ProblemDetail`) uten interne detaljer i respons.

Typiske statuser:

- `400 Bad Request`: valideringsfeil, ugyldig JSON, ugyldig UUID-format.
- `404 Not Found`: fil finnes ikke.
- `405 Method Not Allowed`: ikke støttet HTTP-metode.
- `500 Internal Server Error`: uventede serverfeil.

## Storage

### Produksjon / ikke-lokal

`AzureBlobAdapter` er aktiv når profil **ikke** er `local-staging`:

- Leser:
  - `fint.azure.storage-account.connection-string`
  - `fint.azure.storage.container-blob.name`

### Lokal (`local-staging`)

`InMemoryBlobAdapter` er aktiv i `local-staging`, så Azure-credentials trengs ikke lokalt.

## Sikkerhet

Prosjektet bruker `flyt-web-resource-server`.

Default profiler i `application.yaml`:

- `flyt-kafka`
- `flyt-logging`
- `flyt-web-resource-server`

Lokal bypass av auth/JWT krever **to** betingelser:

1. Profil `local-staging`
2. Property `novari.flyt.file-service.local-security.permit-all-enabled=true`

Denne styres i `LocalPermitAllSecurityConfiguration`.

I `application-local-staging.yaml` er følgende også satt:

- `novari.flyt.web-resource-server.security.api.internal.enabled=false`
- `novari.flyt.web-resource-server.security.api.internal-client.enabled=false`
- `novari.flyt.web-resource-server.security.api.external.enabled=false`

## Logging og observability

- Logging: `kotlin-logging` (`io.github.oshai:kotlin-logging-jvm`)
- Log format: JSON via `logback.xml` + `logstash-logback-encoder`
- Actuator: helse og metrics (`/actuator/health`, `/actuator/prometheus`)

## Konfigurasjon

| Property | Beskrivelse |
| --- | --- |
| `fint.application-id` | App-id, default `fint-flyt-file-service`. |
| `novari.flyt.file-service.time-to-keep-files-in-days` | Retention-vindu for opprydding (default 180). |
| `fint.azure.storage-account.connection-string` | Azure connection string (ikke nødvendig i `local-staging`). |
| `fint.azure.storage.container-blob.name` | Azure container-navn (ikke nødvendig i `local-staging`). |
| `novari.flyt.file-service.local-security.permit-all-enabled` | Lokal `permitAll` når `local-staging`. |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids` | Autoriserte interne klient-id-er. |

## Lokal kjøring

Forutsetninger:

- Java 25+
- Gradle wrapper (`./gradlew`)
- Lokal Kafka på `localhost:9092` (for Kafka-relaterte beans)

Kommandoer:

```shell
./gradlew clean build
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local-staging'
```

Med `local-staging` bruker tjenesten in-memory storage.

## Eksempel på manuell test

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
