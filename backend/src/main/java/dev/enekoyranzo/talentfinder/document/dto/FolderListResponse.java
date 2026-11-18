package dev.enekoyranzo.talentfinder.document.dto;

import java.util.List;

/** unfiledCount = documentos sin carpeta ("General"). */
public record FolderListResponse(long unfiledCount, List<FolderResponse> folders) {
}