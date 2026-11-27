package dev.enekoyranzo.talentfinder.document.dto;

import dev.enekoyranzo.talentfinder.document.Document;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        String filename,
        long sizeBytes,
        Long folderId,
        Instant createdAt,
        String indexStatus,
        String indexError
) {
    // No expone storageKey ni sha256: son detalles internos.
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(d.getId(), d.getOriginalFilename(), d.getSizeBytes(),
                d.getFolderId(), d.getCreatedAt(), d.getIndexStatus().name(), d.getIndexError());
    }
}