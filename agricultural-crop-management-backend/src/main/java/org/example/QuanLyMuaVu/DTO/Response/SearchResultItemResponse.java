package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItemResponse {
    private String type;
    private Long id;
    private String title;
    private String subtitle;
    private String route;
    private Map<String, Object> extra;
}
