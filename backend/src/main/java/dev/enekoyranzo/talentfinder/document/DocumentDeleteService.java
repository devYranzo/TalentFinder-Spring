package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.storage.CvStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentDeleteService {

    private final DocumentRepository documents;
    private final CvStorage storage;
    private final TransactionTemplate tx;

    public DocumentDeleteService(DocumentRepository documents, CvStorage storage, TransactionTemplate tx) {
        this.documents = documents;
        this.storage = storage;
        this.tx = tx;
    }

    /**
     * Orden: primero el objeto del almacén, después la fila.
     * Si falla la segunda operación, quedan una fila sin archivo (el usuario repite el borrado),
     * no un archivo con datos personales que nadie ve. Es el fallo menos grave para el RGPD.
     * Es idempotente: se puede repetir hasta que termine.
     *
     * Sin @Transactional a propósito: no se mantiene una conexión de BD abierta durante la llamada al almacén.
     */
    public void delete(Long organizationId, Long documentId) {
        // 404 también cuando el documento existe en otra organización: no se revela su existencia.
        Document doc = documents.findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado."));

        // Si el almacén falla, lanza StorageException (503) y la fila no se toca.
        storage.delete(doc.getStorageKey());

        // TODO (búsqueda semántica): aquí también hay que borrar los vectores de este documento.

        tx.executeWithoutResult(status -> documents.delete(doc));
    }
}