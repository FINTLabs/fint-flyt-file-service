# Architecture

fint-flyt-file-service stores the files that come with FLYT instances. Gateways upload the files when an instance is
received, and archive-gateway downloads them when it archives the instance. The files are deleted when the instance is
deleted, and by a daily cleanup.

## C4 level 1: System context

```mermaid
C4Context
    title System context for fint-flyt-file-service

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")

    System_Ext(gateways, "Gateways", "Receive instances from the source applications", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    System_Ext(archive, "fint-flyt-archive-gateway", "Archives instances in FINT", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    System(files, "fint-flyt-file-service", "Stores the files of FLYT instances", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    System_Ext(history, "fint-flyt-history-service", "Deletes instances", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    SystemDb_Ext(azure, "Azure Blob Storage", "One container per namespace", $bgColor="#C4B5FD", $fontColor="#1F2937", $borderColor="#8B5CF6")
    System_Ext(nam, "NAM", "Issues the client tokens", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")

    Rel(gateways, files, "Uploads", "HTTP")
    Rel(archive, files, "Downloads", "HTTP")
    Rel(history, files, "instance-deleted", "Kafka")
    Rel(files, azure, "Stores")
    Rel(files, nam, "Validates tokens", "JWKS")

    UpdateRelStyle(gateways, files, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(archive, files, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(history, files, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-47", $offsetY="-35")
    UpdateRelStyle(files, azure, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(files, nam, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
```

## C4 level 2: Container

One Spring Boot application. The files are in Azure Blob Storage, with an in-memory store for `local-staging`.

```mermaid
C4Container
    title Container diagram for fint-flyt-file-service

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")

    Container_Ext(clients, "FLYT services", "Spring Boot", "Gateways and archive-gateway, with the file client", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    ContainerQueue_Ext(kafka, "Kafka", "Topic", "instance-deleted", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    Container(app, "fint-flyt-file-service", "Kotlin, Spring Boot 4, Java 25", "/api/intern-klient/filer, for the file client only.", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    Container_Ext(prometheus, "Prometheus", "Metrics", "/actuator/prometheus", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    ContainerDb_Ext(azure, "Azure Blob Storage", "Blob container", "File content and metadata", $bgColor="#C4B5FD", $fontColor="#1F2937", $borderColor="#8B5CF6")

    Rel(clients, app, "Files", "HTTP")
    Rel(kafka, app, "Consumes")
    Rel(prometheus, app, "Scrapes", "HTTP")
    Rel(app, azure, "Reads and writes")

    UpdateRelStyle(clients, app, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(kafka, app, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(prometheus, app, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(app, azure, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
```

flyt-security-starter only lets the namespace's file client (`fint.sso.client-id`), which the gateways and
archive-gateway use, call the API.
`/actuator/health/**` and `/actuator/prometheus` are open; everything else is denied.

## C4 level 3: Components

```mermaid
C4Component
    title Components in fint-flyt-file-service

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")

    Component(controller, "FileController", "Spring MVC", "POST and GET under /api/intern-klient/filer", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    ContainerQueue_Ext(kafka, "Kafka", "Topic", "instance-deleted, in group fint-flyt-file-service", $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    Component(service, "FileService", "Service", "Names, stores and fetches files", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    Component(listener, "InstanceDeletedListener", "@KafkaListener", "Deletes the files of a deleted instance", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    Component(repository, "FileRepository", "Repository", "Storage through BlobStorageAdapter", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    Component(cleanup, "FileCleanupService", "Scheduled", "Deletes old files daily", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    Component(azure, "AzureBlobAdapter", "Azure SDK", "In-memory adapter in local-staging", $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")

    Rel(controller, service, "Uses")
    Rel(kafka, listener, "Consumes")
    Rel(service, repository, "Stores")
    Rel(listener, repository, "Deletes")
    Rel(repository, azure, "Uses")
    Rel(cleanup, repository, "Deletes")

    UpdateRelStyle(controller, service, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(kafka, listener, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(service, repository, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(listener, repository, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(repository, azure, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(cleanup, repository, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
```

A record on `instance-deleted` without instance flow headers is skipped. A failed deletion is logged and skipped, with
no retries, so the daily cleanup removes what is left.
