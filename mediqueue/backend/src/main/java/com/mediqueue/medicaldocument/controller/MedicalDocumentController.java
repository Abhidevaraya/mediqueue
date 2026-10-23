package com.mediqueue.medicaldocument.controller;

import com.mediqueue.common.ApiResponse;
import com.mediqueue.common.PageResponse;
import com.mediqueue.medicaldocument.dto.MedicalDocumentResponse;
import com.mediqueue.medicaldocument.enums.DocumentCategory;
import com.mediqueue.medicaldocument.service.MedicalDocumentService;
import com.mediqueue.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Medical Documents", description = "Secure private medical document upload, metadata, and authorized streaming downloads")
public class MedicalDocumentController {

    private final MedicalDocumentService documentService;

    @PostMapping(value = "/medical-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a secure medical document (PDF, PNG, JPEG)")
    public ResponseEntity<ApiResponse<MedicalDocumentResponse>> uploadDocument(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam("patientId") Long patientId,
            @RequestParam(value = "appointmentId", required = false) Long appointmentId,
            @RequestParam(value = "consultationId", required = false) Long consultationId,
            @RequestParam(value = "category", required = false) DocumentCategory category,
            @RequestParam("file") MultipartFile file
    ) {
        MedicalDocumentResponse response = documentService.uploadDocument(
                principal, patientId, appointmentId, consultationId, category, file
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Medical document uploaded successfully"));
    }

    @GetMapping("/medical-documents/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get authenticated patient's medical documents with pagination")
    public ResponseEntity<ApiResponse<PageResponse<MedicalDocumentResponse>>> getMyDocuments(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<MedicalDocumentResponse> page = documentService.getPatientDocuments(principal, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page), "Medical documents retrieved"));
    }

    @GetMapping("/medical-documents/{id}")
    @Operation(summary = "Get medical document metadata by ID")
    public ResponseEntity<ApiResponse<MedicalDocumentResponse>> getDocumentMetadata(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        MedicalDocumentResponse response = documentService.getDocumentById(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Document metadata retrieved"));
    }

    @GetMapping("/medical-documents/{id}/download")
    @Operation(summary = "Secure streaming download of medical document with safe headers")
    public ResponseEntity<Resource> downloadDocument(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        MedicalDocumentService.DocumentDownloadPayload payload = documentService.downloadDocument(principal, id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.filename() + "\"")
                .header("Content-Security-Policy", "default-src 'none'")
                .header("X-Content-Type-Options", "nosniff")
                .body(payload.resource());
    }

    @DeleteMapping("/medical-documents/{id}")
    @Operation(summary = "Soft delete medical document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long id
    ) {
        documentService.deleteDocument(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Medical document deleted successfully"));
    }
}
