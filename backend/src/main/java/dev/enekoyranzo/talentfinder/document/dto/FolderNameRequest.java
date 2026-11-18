package dev.enekoyranzo.talentfinder.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Sirve para crear y para renombrar. */
public record FolderNameRequest(
        @NotBlank @Size(max = 100) String name
) {
}