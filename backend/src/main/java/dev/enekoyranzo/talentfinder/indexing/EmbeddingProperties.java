package dev.enekoyranzo.talentfinder.indexing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingProperties(
        String provider,
        @NotBlank String modelId,
        @Min(1) int dimensions,
        boolean verifyOnStartup
) {
}