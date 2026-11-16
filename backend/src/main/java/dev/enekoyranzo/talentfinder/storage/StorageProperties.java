package dev.enekoyranzo.talentfinder.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @NotBlank String region,
        @NotBlank String bucket,
        // Vacío = AWS S3 real. Con valor = servicio compatible (SeaweedFS en desarrollo).
        String endpoint,
        // Vacías = el SDK busca credenciales por su cuenta (rol de IAM en AWS).
        String accessKey,
        String secretKey
) {

    // Evita que las claves acaben en un log si alguien imprime este objeto.
    @Override
    public String toString() {
        return "StorageProperties[region=" + region + ", bucket=" + bucket
                + ", endpoint=" + endpoint + ", credentials=***]";
    }
}