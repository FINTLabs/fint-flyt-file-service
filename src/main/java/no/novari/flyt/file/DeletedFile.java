package no.novari.flyt.file;

import java.time.OffsetDateTime;

public record DeletedFile(String name, OffsetDateTime deletedAt) {}