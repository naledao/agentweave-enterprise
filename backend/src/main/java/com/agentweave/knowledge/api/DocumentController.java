package com.agentweave.knowledge.api;

import com.agentweave.knowledge.application.DocumentApplicationService;
import com.agentweave.knowledge.application.DocumentIndexingService;
import com.agentweave.knowledge.dto.CreateDocumentRequest;
import com.agentweave.knowledge.dto.DocumentDetailResponse;
import com.agentweave.knowledge.dto.DocumentListResponse;
import com.agentweave.knowledge.dto.DocumentQueryRequest;
import com.agentweave.knowledge.dto.DocumentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/v1/documents")
@Validated
public class DocumentController {

    private final DocumentApplicationService documentApplicationService;
    private final DocumentIndexingService documentIndexingService;

    public DocumentController(
            DocumentApplicationService documentApplicationService,
            DocumentIndexingService documentIndexingService) {
        this.documentApplicationService = documentApplicationService;
        this.documentIndexingService = documentIndexingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('knowledge:document:upload') or hasAuthority('ROLE_ADMIN')")
    public DocumentResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam @NotBlank String source,
            @RequestParam @NotBlank String businessDomain,
            @RequestParam @NotBlank String documentType,
            @RequestParam @NotBlank String permissionLevel,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant effectiveFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant effectiveTo,
            @RequestParam(required = false) List<String> tags) {
        CreateDocumentRequest request = new CreateDocumentRequest(
                source,
                businessDomain,
                documentType,
                permissionLevel,
                effectiveFrom,
                effectiveTo,
                tags);
        return documentApplicationService.upload(file, request);
    }

    @GetMapping
    public DocumentListResponse list(@Valid DocumentQueryRequest request) {
        return documentApplicationService.list(request);
    }

    @GetMapping("/{documentId}")
    public DocumentDetailResponse get(@PathVariable UUID documentId) {
        return documentApplicationService.get(documentId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('knowledge:document:delete') or hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable UUID documentId) {
        documentApplicationService.delete(documentId);
    }

    @PostMapping("/{documentId}/parse")
    @PreAuthorize("hasAuthority('knowledge:document:parse') or hasAuthority('ROLE_ADMIN')")
    public DocumentResponse parse(@PathVariable UUID documentId) {
        return documentApplicationService.parseDocument(documentId);
    }

    @PostMapping("/{documentId}/index")
    @PreAuthorize("hasAuthority('knowledge:document:index') or hasAuthority('ROLE_ADMIN')")
    public DocumentResponse index(@PathVariable UUID documentId) {
        return documentIndexingService.indexDocument(documentId);
    }

    @PostMapping("/{documentId}/reindex")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('knowledge:document:index') or hasAuthority('ROLE_ADMIN')")
    public DocumentResponse reindex(@PathVariable UUID documentId) {
        return documentIndexingService.reindexDocument(documentId);
    }
}
