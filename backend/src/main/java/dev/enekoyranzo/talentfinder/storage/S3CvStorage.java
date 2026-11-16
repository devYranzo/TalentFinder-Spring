package dev.enekoyranzo.talentfinder.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class S3CvStorage implements CvStorage {

    private static final Logger log = LoggerFactory.getLogger(S3CvStorage.class);

    // Formato exacto de las claves que genera CvStorage.newKey(): org_{id}/{uuid}
    private static final Pattern KEY_PATTERN = Pattern.compile(
            "^org_[0-9]+/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final S3Client s3;
    private final String bucket;

    public S3CvStorage(S3Client s3, StorageProperties props) {
        this.s3 = s3;
        this.bucket = props.bucket();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        requireValidKey(key);
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (SdkException e) {
            log.error("Error al escribir el objeto {}", key, e);
            throw new StorageException("No se pudo guardar el archivo.", e);
        }
    }

    @Override
    public Optional<byte[]> get(String key) {
        requireValidKey(key);
        try {
            ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(response.asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            log.error("Error al leer el objeto {}", key, e);
            throw new StorageException("No se pudo leer el archivo.", e);
        }
    }

    @Override
    public void delete(String key) {
        requireValidKey(key);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.error("Error al borrar el objeto {}", key, e);
            throw new StorageException("No se pudo borrar el archivo.", e);
        }
    }

    private static void requireValidKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Clave de almacenamiento no válida.");
        }
    }
}