package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.audit.AuditService;
import dev.enekoyranzo.talentfinder.storage.CvStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    // Debe coincidir con el CHECK de la tabla documents (10 MB).
    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final DocumentRepository documents;
    private final FolderRepository folders;
    private final CvStorage storage;
    private final PdfValidator validator;
    private final AuditService audit;
    private final TransactionTemplate tx;

    public DocumentService(DocumentRepository documents, FolderRepository folders,
                           CvStorage storage, PdfValidator validator,
                           AuditService audit, TransactionTemplate tx) {
        this.documents = documents;
        this.folders = folders;
        this.storage = storage;
        this.validator = validator;
        this.audit = audit;
        this.tx = tx;
    }

    /**
     * Orden: validar, hash, duplicados, subir al almacén, insertar fila.
     * No hay transacción que abarque el almacén: si la inserción falla se borra el objeto subido.
     */
    public Document upload(Long organizationId, Long userId, MultipartFile file,
                           Long folderId, String ip) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el archivo.");
        }

        String filename = sanitizeFilename(file.getOriginalFilename());

        // 404 también si la carpeta es de otra organización: no se revela su existencia.
        if (folderId != null && folders.findByIdAndOrganizationId(folderId, organizationId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "El PDF supera los 10 MB.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("No se pudo leer el archivo subido", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo.");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "El PDF supera los 10 MB.");
        }

        try {
            validator.validate(content);
        } catch (InvalidDocumentException e) {
            audit.record(organizationId, userId, "DOCUMENT_UPLOAD", "REJECTED", ip, "reason=" + e.getCode());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        String hash = sha256(content);
        if (documents.existsByOrganizationIdAndSha256(organizationId, hash)) {
            audit.record(organizationId, userId, "DOCUMENT_UPLOAD", "DUPLICATE", ip, null);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CV ya existe en tu organización.");
        }

        String key = CvStorage.newKey(organizationId);
        storage.put(key, content, "application/pdf");

        Document saved;
        try {
            saved = tx.execute(status -> documents.saveAndFlush(new Document(
                    organizationId, folderId, key, filename, content.length, hash, userId)));
        } catch (RuntimeException e) {
            compensate(key);
            if (e instanceof DataIntegrityViolationException) {
                // Duplicado concurrente o carpeta borrada entre la comprobación y la inserción.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo guardar el documento.");
            }
            throw e;
        }

        // Sin nombre de archivo: puede contener el nombre de una persona.
        audit.record(organizationId, userId, "DOCUMENT_UPLOAD", "SUCCESS", ip, "document=" + saved.getId());
        return saved;
    }

    // --------------------------------------------------------------- helpers
    private void compensate(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            // Objeto huérfano: pendiente una limpieza periódica que concilie almacén y base de datos.
            log.error("Objeto huérfano en el almacén tras un fallo de inserción: {}", key, e);
        }
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten archivos PDF.");
        }

        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        // Fuera caracteres de control y de formato (incluidos los que invierten el texto
        // y sirven para disfrazar extensiones).
        name = name.replaceAll("[\\p{Cntrl}\\p{Cf}]", "").trim();

        if (name.length() < 5 || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten archivos PDF.");
        }
        if (name.length() > 255) {
            name = name.substring(0, 251) + ".pdf";
        }
        return name;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}