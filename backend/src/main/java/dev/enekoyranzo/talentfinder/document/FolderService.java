package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.document.dto.FolderListResponse;
import dev.enekoyranzo.talentfinder.document.dto.FolderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FolderService {

    private static final String RESERVED_NAME = "general";

    private final FolderRepository folders;
    private final DocumentRepository documents;

    public FolderService(FolderRepository folders, DocumentRepository documents) {
        this.folders = folders;
        this.documents = documents;
    }

    @Transactional(readOnly = true)
    public FolderListResponse list(Long organizationId) {
        Map<Long, Long> counts = new HashMap<>();
        long unfiled = 0;
        for (DocumentRepository.FolderCount c : documents.countByFolder(organizationId)) {
            if (c.getFolderId() == null) {
                unfiled = c.getTotal();
            } else {
                counts.put(c.getFolderId(), c.getTotal());
            }
        }

        List<FolderResponse> items = folders.findAllByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(f -> FolderResponse.from(f, counts.getOrDefault(f.getId(), 0L)))
                .toList();

        return new FolderListResponse(unfiled, items);
    }

    @Transactional
    public Folder create(Long organizationId, String rawName) {
        String name = normalizeName(rawName);

        if (folders.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carpeta con ese nombre.");
        }

        // Si dos peticiones simultáneas pasan la comprobación, el índice único de la BD
        // rechaza la segunda (DataIntegrityViolationException -> 409 en ApiExceptionHandler).
        return folders.saveAndFlush(new Folder(organizationId, name));
    }

    @Transactional
    public Folder rename(Long organizationId, Long id, String rawName) {
        Folder folder = find(organizationId, id);
        String name = normalizeName(rawName);

        // Cambiar solo mayúsculas o minúsculas del propio nombre está permitido.
        if (!name.equalsIgnoreCase(folder.getName())
                && folders.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carpeta con ese nombre.");
        }

        folder.setName(name);
        return folders.saveAndFlush(folder);
    }

    /**
     * Borra la carpeta. Sus documentos NO se borran: la FK ON DELETE SET NULL los pasa a "General".
     * @return cuántos documentos han pasado a "General"
     */
    @Transactional
    public long delete(Long organizationId, Long id) {
        Folder folder = find(organizationId, id);
        long moved = documents.countByOrganizationIdAndFolderId(organizationId, folder.getId());

        folders.delete(folder);
        folders.flush();

        return moved;
    }

    // --------------------------------------------------------------- helpers
    private Folder find(Long organizationId, Long id) {
        // 404 también cuando la carpeta existe en otra organización: no se revela su existencia.
        return folders.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carpeta no encontrada."));
    }

    private static String normalizeName(String raw) {
        // Fuera caracteres de control y de formato invisibles.
        String name = raw == null ? "" : raw.replaceAll("[\\p{Cntrl}\\p{Cf}]", "").trim();

        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío.");
        }
        if (name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es demasiado largo.");
        }
        if (name.equalsIgnoreCase(RESERVED_NAME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "\"General\" es un nombre reservado para los CVs sin carpeta.");
        }
        return name;
    }
}