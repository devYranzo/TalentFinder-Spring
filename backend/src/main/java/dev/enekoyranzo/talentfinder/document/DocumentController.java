package dev.enekoyranzo.talentfinder.document;

import dev.enekoyranzo.talentfinder.common.RequestUtils;
import dev.enekoyranzo.talentfinder.document.dto.DocumentResponse;
import dev.enekoyranzo.talentfinder.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@PreAuthorize("hasRole('RECRUITER')")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
}