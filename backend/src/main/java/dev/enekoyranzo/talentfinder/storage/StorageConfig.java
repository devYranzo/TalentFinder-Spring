package dev.enekoyranzo.talentfinder.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean(destroyMethod = "close")
    S3Client s3Client(StorageProperties props) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(props.region()))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                // Las versiones recientes del SDK calculan checksums extra por defecto, y algunos
                // servicios compatibles con S3 no los soportan. Solo se calculan si el servicio los exige.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);

        // Servicio compatible (SeaweedFS): endpoint propio y bucket en la ruta, no en el host.
        if (StringUtils.hasText(props.endpoint())) {
            builder.endpointOverride(URI.create(props.endpoint().trim()))
                    .forcePathStyle(true);
        }

        // Claves explícitas en desarrollo. Sin ellas, el SDK usa su cadena por defecto
        // (variables de entorno, rol de IAM...), que es lo que queremos en AWS.
        if (StringUtils.hasText(props.accessKey()) && StringUtils.hasText(props.secretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.accessKey().trim(), props.secretKey().trim())));
        }

        return builder.build();
    }
}