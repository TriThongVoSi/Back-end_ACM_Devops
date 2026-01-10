package org.example.QuanLyMuaVu.Controller.Admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Common.ApiResponse;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.DTO.Response.PlotResponse;
import org.example.QuanLyMuaVu.DTO.Response.SeasonResponse;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.Season;
import org.example.QuanLyMuaVu.Enums.PlotStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin REST endpoints for system-wide plot management.
 * Returns all plots across all farms for administrative purposes.
 */
@RestController
@RequestMapping("/api/v1/admin/plots")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Plots", description = "Admin endpoints for system-wide plot management")
public class AdminPlotController {

    PlotRepository plotRepository;
    SeasonRepository seasonRepository;

    @Operation(summary = "List all plots (Admin)", description = "Get paginated list of all plots across all farms")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ApiResponse<PageResponse<PlotResponse>> listAllPlots(
            @Parameter(description = "Filter by farm ID") @RequestParam(value = "farmId", required = false) Integer farmId,
            @Parameter(description = "Search by name") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "Page index (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(value = "size", defaultValue = "20") int size) {

        List<Plot> allPlots;

        // Filter by farmId if provided
        if (farmId != null) {
            allPlots = plotRepository.findAllByFarm_Id(farmId);
        } else {
            allPlots = plotRepository.findAll();
        }

        // Apply keyword filter if provided
        if (keyword != null && !keyword.trim().isEmpty()) {
            String keywordLower = keyword.toLowerCase();
            allPlots = allPlots.stream()
                    .filter(p -> p.getPlotName() != null && p.getPlotName().toLowerCase().contains(keywordLower))
                    .collect(Collectors.toList());
        }

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, allPlots.size());
        List<Plot> pageContent = start < allPlots.size() ? allPlots.subList(start, end) : List.of();

        List<PlotResponse> content = pageContent.stream()
                .map(this::toPlotResponse)
                .collect(Collectors.toList());

        // Build PageResponse
        PageResponse<PlotResponse> pageResponse = new PageResponse<>();
        pageResponse.setItems(content);
        pageResponse.setPage(page);
        pageResponse.setSize(size);
        pageResponse.setTotalElements(allPlots.size());
        pageResponse.setTotalPages((int) Math.ceil((double) allPlots.size() / size));

        return ApiResponse.success(pageResponse);
    }

    @Operation(summary = "Get plot detail (Admin)", description = "Get detailed information about a specific plot")
    @GetMapping("/{id}")
    public ApiResponse<PlotResponse> getPlot(@PathVariable Integer id) {
        Plot plot = plotRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));
        return ApiResponse.success(toPlotResponse(plot));
    }

    @Operation(summary = "Get plot seasons (Admin)", description = "Get all seasons for a specific plot")
    @GetMapping("/{id}/seasons")
    public ApiResponse<List<SeasonResponse>> listPlotSeasons(@PathVariable Integer id) {
        Plot plot = plotRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        List<Season> seasons = seasonRepository.findByPlot(plot);

        return ApiResponse.success(seasons.stream()
                .map(this::toSeasonResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Map Plot entity to PlotResponse DTO.
     * Matches the PlotResponse structure in Main Backend.
     */
    private PlotResponse toPlotResponse(Plot p) {
        // Parse status string to PlotStatus enum if possible
        PlotStatus plotStatus = null;
        if (p.getStatus() != null) {
            try {
                plotStatus = PlotStatus.valueOf(p.getStatus());
            } catch (IllegalArgumentException e) {
                // Keep as null if status doesn't match enum
            }
        }

        return PlotResponse.builder()
                .id(p.getId())
                .farmId(p.getFarm() != null ? p.getFarm().getId() : null)
                .farmName(p.getFarm() != null ? p.getFarm().getName() : null)
                .plotName(p.getPlotName())
                .area(p.getArea())
                .soilType(p.getSoilType())
                .status(plotStatus)
                .build();
    }

    /**
     * Map Season entity to SeasonResponse DTO.
     */
    private SeasonResponse toSeasonResponse(Season s) {
        return SeasonResponse.builder()
                .id(s.getId())
                .seasonName(s.getSeasonName())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus() != null ? s.getStatus().getCode() : null)
                .plotId(s.getPlot() != null ? s.getPlot().getId() : null)
                .cropId(s.getCrop() != null ? s.getCrop().getId() : null)
                .varietyId(s.getVariety() != null ? s.getVariety().getId() : null)
                .plannedHarvestDate(s.getPlannedHarvestDate())
                .expectedYieldKg(s.getExpectedYieldKg())
                .actualYieldKg(s.getActualYieldKg())
                .build();
    }
}
