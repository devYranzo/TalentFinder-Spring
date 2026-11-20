package dev.enekoyranzo.talentfinder.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class S3CvStorageTest {

    private S3Client s3;
    private S3CvStorage storage;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        storage = new S3CvStorage(s3, new StorageProperties("us-east-1", "cvs", null, null, null));
    }

    // ---------------------------------------------------------- claves
    @Test
    void newKeyGeneraClavesConElFormatoEsperadoYDistintasCadaVez() {
        String a = CvStorage.newKey(9L);
        String b = CvStorage.newKey(9L);

        assertThat(a).matches("^org_9/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(a).isNotEqualTo(b);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "../secreto",
            "org_1/../../etc/passwd",
            "/org_1/123e4567-e89b-12d3-a456-426614174000",
            "org_1/123e4567-e89b-12d3-a456-426614174000/extra",
            "org_abc/123e4567-e89b-12d3-a456-426614174000",
            "org_1/123E4567-E89B-12D3-A456-426614174000",
            "org_1/123e4567-e89b-12d3-a456-426614174000\n",
            "org_1/no-es-un-uuid",
            "cualquier-cosa"
    })
    void rechazaClavesNoValidasEnTodasLasOperacionesSinTocarElAlmacen(String key) {
        assertThatThrownBy(() -> storage.put(key, new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.get(key)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.delete(key)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(s3);
    }

    // -------------------------------------------------------------- put
    @Test
    void putEnviaBucketClaveTipoYTamanoCorrectos() {
        String key = CvStorage.newKey(7L);

        storage.put(key, new byte[]{1, 2, 3}, "application/pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("cvs");
        assertThat(captor.getValue().key()).isEqualTo(key);
        assertThat(captor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(captor.getValue().contentLength()).isEqualTo(3L);
    }

    @Test
    void putConvierteUnFalloDelSdkEnStorageExceptionSinFiltrarLaClave() {
        String key = CvStorage.newKey(7L);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("fallo simulado"));

        assertThatThrownBy(() -> storage.put(key, new byte[]{1}, "application/pdf"))
                .isInstanceOf(StorageException.class)
                .hasMessageNotContaining(key);
    }

    // -------------------------------------------------------------- get
    @Test
    void getDevuelveElContenidoSiElObjetoExiste() {
        String key = CvStorage.newKey(7L);
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[]{9, 8}));

        Optional<byte[]> result = storage.get(key);

        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(9, 8);
    }

    @Test
    void getDevuelveVacioSiElObjetoNoExiste() {
        String key = CvStorage.newKey(7L);
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThat(storage.get(key)).isEmpty();
    }

    @Test
    void getConvierteUnFalloDelSdkEnStorageException() {
        String key = CvStorage.newKey(7L);
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(SdkClientException.create("fallo simulado"));

        assertThatThrownBy(() -> storage.get(key)).isInstanceOf(StorageException.class);
    }

    // ----------------------------------------------------------- delete
    @Test
    void deleteBorraElObjetoDelBucketCorrecto() {
        String key = CvStorage.newKey(7L);

        storage.delete(key);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("cvs");
        assertThat(captor.getValue().key()).isEqualTo(key);
    }

    @Test
    void deleteConvierteUnFalloDelSdkEnStorageException() {
        String key = CvStorage.newKey(7L);
        when(s3.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("fallo simulado"));

        assertThatThrownBy(() -> storage.delete(key)).isInstanceOf(StorageException.class);
    }
}