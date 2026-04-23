# FINT Flyt File Service

Kotlin-basert Spring Boot-tjeneste for opplasting og henting av filer i Flyt.
Tjenesten eksponerer et internt HTTP-API, lagrer filer i utskiftbar lagring
(Azure i produksjon, i minne lokalt), bruker cache for raske oppslag og sletter
filer både gjennom planlagte jobber og Kafka-hendelser.

## Høydepunkter

- Spring MVC (`spring-boot-starter-web`).
- Domenemodell i Kotlin.
- Global feilbehandling med `ProblemDetail`.
- JSON-logging via Logback + logstash encoder.
- Lokal utvikling uten Azure ved bruk av en lagringsadapter i minnet.

## Arkitektur

| Komponent | Ansvar |
| --- | --- |
| `FileController` | Endepunkter for `POST` og `GET` av filer. |
| `FileService` | Forretningsflyt, cache-oppslag og fallback til repository. |
| `FileRepository` | Lagringsoperasjoner gjennom `BlobStorageAdapter`. |
| `BlobStorageAdapter` | Lagringsabstraksjon (port). |
| `AzureBlobAdapter` | Azure Blob-implementasjon (`!local-staging`). |
| `InMemoryBlobAdapter` | Lokal mock-lagring (`local-staging`). |
| `GlobalExceptionHandler` | Standardiserte feilresponser med `ProblemDetail`. |
| `FileCleanupService` | Daglig opprydding av gamle filer. |
| `InstanceDeletedConsumerConfiguration` | Kafka-lytter for sletting av filer ved `instance-deleted`-hendelser. |

## Pakkestruktur

- `no.novari.flyt.files.api`
- `no.novari.flyt.files.application`
- `no.novari.flyt.files.domain`
- `no.novari.flyt.files.infrastructure.storage`
- `no.novari.flyt.files.infrastructure.storage.azure`
- `no.novari.flyt.files.infrastructure.storage.inmemory`
- `no.novari.flyt.files.infrastructure.kafka`
- `no.novari.flyt.api.error`

## HTTP-API

Basesti: `/api/intern-klient/filer`

| Metode | Path | Beskrivelse | Respons |
| --- | --- | --- | --- |
| `POST` | `/api/intern-klient/filer` | Lagrer en fil og returnerer en generert UUID. | `201 Created` + UUID i responsen |
| `GET` | `/api/intern-klient/filer/{fileId}` | Henter en fil ved hjelp av UUID. | `200 OK` + `FilePayload` |

`FilePayload` (forespørsel/respons):

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

`contents` er base64 i JSON og mappes internt til `ByteArray`.

## Feilhåndtering

API-et returnerer `application/problem+json` (`ProblemDetail`) uten interne
detaljer i responsen.

Vanlige statuser:

- `400 Bad Request`: valideringsfeil, ugyldig JSON, ugyldig UUID-format.
- `404 Not Found`: filen finnes ikke.
- `405 Method Not Allowed`: ikke-støttet HTTP-metode.
- `500 Internal Server Error`: uventede serverfeil.

## Lagring

### Produksjon / ikke-lokal

`AzureBlobAdapter` er aktiv når profilen **ikke** er `local-staging`:

- Leser:
  - `fint.azure.storage-account.connection-string`
  - `fint.azure.storage.container-blob.name`

### Lokalt (`local-staging`)

`InMemoryBlobAdapter` er aktiv i `local-staging`, så Azure-legitimasjon er ikke
nødvendige lokalt.

## Sikkerhet

Prosjektet bruker `flyt-web-resource-server`.

Standardprofiler i `application.yaml`:

- `flyt-kafka`
- `flyt-logging`
- `flyt-web-resource-server`

Lokal bypass av auth/JWT krever **to** betingelser:

1. Profilen `local-staging`
2. Egenskapen `novari.flyt.file-service.local-security.permit-all-enabled=true`

Dette styres i `LocalPermitAllSecurityConfiguration`.

Følgende er også satt i `application-local-staging.yaml`:

- `novari.flyt.web-resource-server.security.api.internal.enabled=false`
- `novari.flyt.web-resource-server.security.api.internal-client.enabled=false`
- `novari.flyt.web-resource-server.security.api.external.enabled=false`

## Logging og observabilitet

- Logging: `kotlin-logging` (`io.github.oshai:kotlin-logging-jvm`)
- Loggformat: JSON via `logback.xml` + `logstash-logback-encoder`
- Actuator: helse og metrikker (`/actuator/health`, `/actuator/prometheus`)

## Konfigurasjon

| Egenskap | Beskrivelse |
| --- | --- |
| `fint.application-id` | App-ID, standard er `fint-flyt-file-service`. |
| `novari.flyt.file-service.time-to-keep-files-in-days` | Retensjonsvindu for opprydding (standard 61). |
| `fint.azure.storage-account.connection-string` | Azure connection string (ikke påkrevd i `local-staging`). |
| `fint.azure.storage.container-blob.name` | Navn på Azure-container (ikke påkrevd i `local-staging`). |
| `novari.flyt.file-service.local-security.permit-all-enabled` | Lokal `permitAll` når `local-staging` er aktiv. |
| `novari.flyt.web-resource-server.security.api.internal-client.authorized-client-ids` | Autoriserte interne klient-ID-er. |

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

Med `local-staging` bruker tjenesten lagring i minnet.

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
