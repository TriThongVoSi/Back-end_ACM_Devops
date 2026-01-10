package org.example.QuanLyMuaVu.Controller.Admin;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.AdminDocumentResponse;
import org.example.QuanLyMuaVu.Entity.Document;
import org.example.QuanLyMuaVu.Repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Document Management Controller
 * Provides CRUD operations for system documents (Admin only)
 */
@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final DocumentRepository documentRepository;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * GET /api/v1/admin/documents - List all documents with pagination
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminDocumentResponse>>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        // Use filter query or findAll
        Page<Document> documentPage;
        if (q != null && !q.isEmpty()) {
            // Simple search by title containing (for admin, show all including inactive)
            documentPage = documentRepository.findAll(pageable);
        } else {
            documentPage = documentRepository.findAll(pageable);
        }

        List<AdminDocumentResponse> items = documentPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        PageResponse<AdminDocumentResponse> response = PageResponse.of(documentPage, items);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/admin/documents/{id} - Get document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> getDocumentById(@PathVariable Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(doc)));
    }

    /**
     * POST /api/v1/admin/documents - Create new document
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> createDocument(
            @RequestBody CreateDocumentRequest request) {
        Document doc = Document.builder()
                .title(request.title())
                .description(request.description())
                .url(request.documentUrl())
                .topic(request.documentType()) // Map documentType -> topic
                .isActive("ACTIVE".equalsIgnoreCase(request.status()))
                .isPublic(true)
                .build();
        Document saved = documentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(saved)));
    }

    /**
     * PUT /api/v1/admin/documents/{id} - Update document
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminDocumentResponse>> updateDocument(
            @PathVariable Integer id,
            @RequestBody CreateDocumentRequest request) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        doc.setTitle(request.title());
        doc.setDescription(request.description());
        doc.setUrl(request.documentUrl());
        doc.setTopic(request.documentType());
        doc.setIsActive("ACTIVE".equalsIgnoreCase(request.status()));

        Document updated = documentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(updated)));
    }

    /**
     * DELETE /api/v1/admin/documents/{id} - Soft delete (set inactive)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteDocument(@PathVariable Integer id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        doc.setIsActive(false);
        documentRepository.save(doc);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * DELETE /api/v1/admin/documents/{id}/permanent - Hard delete
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> hardDeleteDocument(@PathVariable Integer id) {
        if (!documentRepository.existsById(id)) {
            throw new RuntimeException("Document not found: " + id);
        }
        documentRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private AdminDocumentResponse mapToResponse(Document doc) {
        return AdminDocumentResponse.builder()
                .id(doc.getId() != null ? doc.getId().longValue() : null)
                .title(doc.getTitle())
                .description(doc.getDescription())
                .documentUrl(doc.getUrl())
                .documentType(doc.getTopic() != null ? doc.getTopic() : "OTHER")
                .status(Boolean.TRUE.equals(doc.getIsActive()) ? "ACTIVE" : "INACTIVE")
                .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().format(DTF) : null)
                .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().format(DTF) : null)
                .createdBy(doc.getCreatedBy())
                .build();
    }

    // Request record for create/update
    record CreateDocumentRequest(
            String title,
            String description,
            String documentUrl,
            String documentType,
            String status) {
    }
}
