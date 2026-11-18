package dev.enekoyranzo.talentfinder.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.enekoyranzo.talentfinder.storage.CvStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentBrowseService {

    private static final Logger log = LoggerFactory.getLogger(DocumentBrowseService.class);

    static final int MAX_PAGE_SIZE = 100;

    public record Download(String filename, byte[] content) {
    }

    private final DocumentRepository documents;
    private final FolderRepository folders;
    private final CvStorage storage;

    public DocumentBrowseService(DocumentRepository documents, FolderRepository folders, CvStorage storage) {
        this.documents = documents;
        this.folders = folders;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Page<Document> list(Long organizationId, Long folderId, boolean unfiled, int page, int size) {
        if (folderId != null && unfiled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usa folderId o unfiled, no los dos a la vez.");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paginación no válida (size entre 1 y " + MAX_PAGE_SIZE + ").");
        }

        // Más reciente primero; el id desempata para que el orden sea estable entre páginas.
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        if (unfiled) {
            return documents.findByOrganizationIdAndFolderIdIsNull(organizationId, pageable);
        }
        if (folderId != null) {
            requireFolder(organizationId, folderId);
            return documents.findByOrganizationIdAndFolderId(organizationId, folderId, pageable);
        }
        return documents.findByOrganizationId(organizationId, pageable);
    }

    /** folderId null = mover a "General". */
    @Transactional
    public Document move(Long organizationId, Long documentId, Long folderId) {
        Document doc = find(organizationId, documentId);

        if (folderId != null) {
            requireFolder(organizationId, folderId);
        }

        doc.setFolderId(folderId);
        return documents.saveAndFlush(doc);
    }

    /**
     * Sin @Transactional a propósito: no se debe mantener una conexión a la base de datos
     * ocupada mientras se espera al almacén.
     */
    public Download download(Long organizationId, Long documentId) {
        Document doc = find(organizationId, documentId);

        byte[] content = storage.get(doc.getStorageKey()).orElseThrow(() -> {
            // Fila sin objeto: incoherencia real entre la base de datos y el almacén.
            log.error("El documento {} existe en la base de datos pero no en el almacén (clave {}).",
                    doc.getId(), doc.getStorageKey());
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo no está disponible.");
        });

        return new Download(doc.getOriginalFilename(), content);
    }

    // --------------------------------------------------------------- helpers
    private Document find(Long organizationId, Long id) {
        // 404 también cuando el documento existe en otra organización: no se revela su existencia.
        return documents.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado."));
    }

    private void requireFolder(Long organizationId, Long folderId) {
        if (folders.findByIdAndOrganizationId(folderId, organizationId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada.");
        }
    }
}