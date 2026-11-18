package dev.enekoyranzo.talentfinder.document.dto;

import dev.enekoyranzo.talentfinder.document.Folder;
import java.time.Instant;

public record FolderResponse(Long id, String name, long documentCount, Instant createdAt) {

    public static FolderResponse from(Folder f, long documentCount) {
        return new FolderResponse(f.getId(), f.getName(), documentCount, f.getCreatedAt());
    }
}