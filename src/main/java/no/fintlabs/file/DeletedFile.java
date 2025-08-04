package no.fintlabs.file;

import java.time.OffsetDateTime;

public record DeletedFile(String name, OffsetDateTime deletedAt) {}