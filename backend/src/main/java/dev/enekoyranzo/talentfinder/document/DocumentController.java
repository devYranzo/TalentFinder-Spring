package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.audit.AuditService;
import dev.enekoyranzo.talentfinder.common.RequestUtils;
import dev.enekoyranzo.talentfinder.document.dto.DocumentPageResponse;
import dev.enekoyranzo.talentfinder.document.dto.DocumentResponse;
import dev.enekoyranzo.talentfinder.document.dto.MoveDocumentRequest;
import dev.enekoyranzo.talentfinder.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

// ADMIN también pasa: la jerarquía de roles lo incluye.
@RestController
@RequestMapping("/documents")
@PreAuthorize("hasRole('RECRUITER')")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentBrowseService browseService;
    private final DocumentDeleteService deleteService;
    private final AuditService audit;

    public DocumentController(DocumentService documentService,
                              DocumentBrowseService browseService,
                              DocumentDeleteService deleteService,
                              AuditService audit) {
        this.documentService = documentService;
        this.browseService = browseService;
        this.deleteService = deleteService;
        this.audit = audit;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @AuthenticationPrincipal AuthenticatedUser actor,
            HttpServletRequest request) {

        Document saved = documentService.upload(
                actor.organizationId(), actor.userId(), file, folderId,
                RequestUtils.clientIp(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(saved));
    }

    @GetMapping
    public DocumentPageResponse list(
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "unfiled", defaultValue = "false") boolean unfiled,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser actor) {

        Page<Document> result = browseService.list(actor.organizationId(), folderId, unfiled, page, size);

        return new DocumentPageResponse(
                result.getContent().stream().map(DocumentResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @PatchMapping("/{id}/folder")
    public DocumentResponse move(@PathVariable Long id,
                                 @RequestBody MoveDocumentRequest body,
                                 @AuthenticationPrincipal AuthenticatedUser actor,
                                 HttpServletRequest request) {
        Document moved = browseService.move(actor.organizationId(), id, body.folderId());

        audit.record(actor.organizationId(), actor.userId(), "DOCUMENT_MOVE", "SUCCESS",
                RequestUtils.clientIp(request),
                "document=" + id + ", folder=" + (body.folderId() == null ? "general" : body.folderId()));

        return DocumentResponse.from(moved);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthenticatedUser actor,
                                           HttpServletRequest request) {
        DocumentBrowseService.Download file = browseService.download(actor.organizationId(), id);

        // Acceso a datos personales de un candidato: queda registrado (id, nunca el nombre).
        audit.record(actor.organizationId(), actor.userId(), "DOCUMENT_DOWNLOAD", "SUCCESS",
                RequestUtils.clientIp(request), "document=" + id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser actor,
                                       HttpServletRequest request) {
        deleteService.delete(actor.organizationId(), id);

        // La auditoría guarda el id, nunca el nombre del archivo.
        audit.record(actor.organizationId(), actor.userId(), "DOCUMENT_DELETE", "SUCCESS",
                RequestUtils.clientIp(request), "document=" + id);

        return ResponseEntity.noContent().build();
    }
}