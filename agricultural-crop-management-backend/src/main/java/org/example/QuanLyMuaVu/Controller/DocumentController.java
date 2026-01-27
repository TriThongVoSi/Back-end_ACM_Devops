package org.example.QuanLyMuaVu.Controller;

import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.DocumentMetaResponse;
import org.example.QuanLyMuaVu.DTO.Response.DocumentResponse;
import org.example.QuanLyMuaVu.Service.DocumentService;
import org.example.QuanLyMuaVu.Util.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Read-only access for farmers to view documents, manage favorites, and track
 * recently opened.
 */
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    /**
     * List documents with filters and tab support
     * GET
     * /api/v1/documents?tab=all|favorites|recent&q=&type=&crop=&stage=&topic=&sort=&page=&size=
     */
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping
    public ApiResponse<PageResponse<DocumentResponse>> list(
            @RequestParam(defaultValue = "all") String tab,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String crop,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "NEWEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = currentUserService.getCurrentUserId();
        PageResponse<DocumentResponse> result = documentService.listDocuments(
                tab, q, type, crop, stage, topic, sort, page, size, userId);
        return ApiResponse.success(result);
    }

    /**
     * Get filter dropdown metadata
     * GET /api/v1/documents/meta
     */
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/meta")
    public ApiResponse<DocumentMetaResponse> getMeta() {
        DocumentMetaResponse meta = documentService.getDocumentMeta();
        return ApiResponse.success(meta);
    }

    /**
     * Get single document by ID
     * GET /api/v1/documents/{id}
     */
    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        DocumentResponse doc = documentService.getById(id, userId);
        return ApiResponse.success(doc);
    }

    /**
     * Record document open (for Recent tab)
     * POST /api/v1/documents/{id}/open
     */
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/{id}/open")
    public ApiResponse<Void> recordOpen(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        documentService.recordOpen(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * Add document to favorites
     * POST /api/v1/documents/{id}/favorite
     */
    @PreAuthorize("hasRole('FARMER')")
    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> addFavorite(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        documentService.addFavorite(id, userId);
        return ApiResponse.success(null);
    }

    /**
     * Remove document from favorites
     * DELETE /api/v1/documents/{id}/favorite
     */
    @PreAuthorize("hasRole('FARMER')")
    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> removeFavorite(@PathVariable Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        documentService.removeFavorite(id, userId);
        return ApiResponse.success(null);
    }
}
