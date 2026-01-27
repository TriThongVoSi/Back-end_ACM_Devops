package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseListResponse {
    List<ExpenseResponse> items;
    int page;
    int size;
    long totalElements;
    int totalPages;

    public static ExpenseListResponse from(PageResponse<ExpenseResponse> page) {
        if (page == null) {
            return ExpenseListResponse.builder()
                    .items(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0)
                    .totalPages(0)
                    .build();
        }
        return ExpenseListResponse.builder()
                .items(page.getItems())
                .page(page.getPage())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
