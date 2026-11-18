package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.audit.AuditService;
import dev.enekoyranzo.talentfinder.common.RequestUtils;
import dev.enekoyranzo.talentfinder.document.dto.FolderListResponse;
import dev.enekoyranzo.talentfinder.document.dto.FolderNameRequest;
import dev.enekoyranzo.talentfinder.document.dto.FolderResponse;
import dev.enekoyranzo.talentfinder.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

// ADMIN también pasa: la jerarquía de roles lo incluye.
@RestController
@RequestMapping("/folders")
@PreAuthorize("hasRole('RECRUITER')")
public class FolderController {

    private final FolderService folderService;
    private final AuditService audit;

    public FolderController(FolderService folderService, AuditService audit) {
        this.folderService = folderService;
        this.audit = audit;
    }

    @GetMapping
    public FolderListResponse list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return folderService.list(actor.organizationId());
    }

    @PostMapping
    public ResponseEntity<FolderResponse> create(@Valid @RequestBody FolderNameRequest body,
                                                 @AuthenticationPrincipal AuthenticatedUser actor,
                                                 HttpServletRequest request) {
        Folder created = folderService.create(actor.organizationId(), body.name());

        // La auditoría guarda el id, no el nombre.
        audit.record(actor.organizationId(), actor.userId(), "FOLDER_CREATE", "SUCCESS",
                RequestUtils.clientIp(request), "folder=" + created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(FolderResponse.from(created, 0));
    }

    @PatchMapping("/{id}")
    public FolderResponse rename(@PathVariable Long id,
                                 @Valid @RequestBody FolderNameRequest body,
                                 @AuthenticationPrincipal AuthenticatedUser actor,
                                 HttpServletRequest request) {
        Folder renamed = folderService.rename(actor.organizationId(), id, body.name());

        audit.record(actor.organizationId(), actor.userId(), "FOLDER_RENAME", "SUCCESS",
                RequestUtils.clientIp(request), "folder=" + id);

        long count = folderService.list(actor.organizationId()).folders().stream()
                .filter(f -> f.id().equals(id)).mapToLong(FolderResponse::documentCount).findFirst().orElse(0);
        return FolderResponse.from(renamed, count);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser actor,
                                       HttpServletRequest request) {
        long moved = folderService.delete(actor.organizationId(), id);

        audit.record(actor.organizationId(), actor.userId(), "FOLDER_DELETE", "SUCCESS",
                RequestUtils.clientIp(request), "folder=" + id + ", documentsMovedToGeneral=" + moved);

        return ResponseEntity.noContent().build();
    }
}