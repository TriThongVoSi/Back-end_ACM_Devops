package org.example.QuanLyMuaVu.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filter options for admin reports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportOptionsResponse {

    private List<SimpleOption> farms;
    private List<PlotOption> plots;
    private List<SimpleOption> crops;
    private List<VarietyOption> varieties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleOption {
        private Integer id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlotOption {
        private Integer id;
        private String name;
        private Integer farmId;
        private String farmName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VarietyOption {
        private Integer id;
        private String name;
        private Integer cropId;
        private String cropName;
    }
}
