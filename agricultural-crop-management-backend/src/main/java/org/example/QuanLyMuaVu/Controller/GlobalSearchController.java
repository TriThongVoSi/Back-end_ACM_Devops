package org.example.QuanLyMuaVu.Controller;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Response.SearchResponse;
import org.example.QuanLyMuaVu.Enums.SearchEntityType;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Service.GlobalSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    @PreAuthorize("hasAnyRole('ADMIN','FARMER')")
    @GetMapping
    public ApiResponse<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String types,
            @RequestParam(defaultValue = "5") int limit) {
        if (q == null || q.trim().length() < 2) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        int safeLimit = Math.max(1, limit);
        Set<SearchEntityType> parsedTypes = parseTypes(types);
        SearchResponse response = globalSearchService.search(q.trim(), parsedTypes, safeLimit);
        return ApiResponse.success(response);
    }

    private Set<SearchEntityType> parseTypes(String rawTypes) {
        if (rawTypes == null || rawTypes.isBlank()) {
            return EnumSet.noneOf(SearchEntityType.class);
        }
        Set<SearchEntityType> parsed = EnumSet.noneOf(SearchEntityType.class);
        for (String token : rawTypes.split(",")) {
            SearchEntityType type = SearchEntityType.fromParam(token);
            if (type != null) {
                parsed.add(type);
            }
        }
        return parsed;
    }
}
