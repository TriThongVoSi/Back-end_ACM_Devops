package org.example.QuanLyMuaVu.Service;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Request.PlotRequest;
import org.example.QuanLyMuaVu.DTO.Response.PlotResponse;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Entity.Plot;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.PlotStatus;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.example.QuanLyMuaVu.Repository.PlotRepository;
import org.example.QuanLyMuaVu.Util.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlotService {

    private final PlotRepository plotRepository;
    private final FarmRepository farmRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<PlotResponse> listPlotsForCurrentFarmer() {
        User currentUser = currentUserService.getCurrentUser();
        // Option 1: Find all plots for user (regardless of farm ownership? Usually plot
        // user = creator)
        // Option 2: Find all plots in farms owned by user
        List<Plot> plots = plotRepository.findAllByFarmUserId(currentUser.getId());
        return plots.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PlotResponse createPlotForCurrentFarmer(PlotRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request.getFarmId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        return createPlotForFarm(request.getFarmId(), request);
    }

    @Transactional(readOnly = true)
    public List<PlotResponse> listPlotsByFarm(Integer farmId) {
        User currentUser = currentUserService.getCurrentUser();

        // Check farm ownership
        Farm farm = farmRepository.findByIdAndUser(farmId, currentUser)
                .orElseThrow(() -> new AccessDeniedException("Access Denied: You do not own this farm."));

        List<Plot> plots = plotRepository.findAllByFarm(farm);
        return plots.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PlotResponse createPlotForFarm(Integer farmId, PlotRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        // Check farm existence and ownership
        Farm farm = farmRepository.findByIdAndUser(farmId, currentUser)
                .orElseThrow(() -> new AccessDeniedException("Access Denied: You do not own this farm."));

        // Check active status
        if (!Boolean.TRUE.equals(farm.getActive())) {
            throw new AppException(ErrorCode.FARM_INACTIVE);
        }

        // Validate status (handled by Enum now)
        String statusCode = (request.getStatus() != null) ? request.getStatus().getCode() : PlotStatus.IDLE.getCode();

        Plot plot = Plot.builder()
                .farm(farm)
                .user(currentUser) // Created by
                .plotName(request.getPlotName())
                .area(request.getArea())
                .soilType(request.getSoilType())
                .status(statusCode) // Store string code
                .build();

        Plot savedPlot = plotRepository.save(plot);
        return toResponse(savedPlot);
    }

    @Transactional(readOnly = true)
    public PlotResponse getPlotForCurrentFarmer(Integer id) {
        User currentUser = currentUserService.getCurrentUser();
        Plot plot = plotRepository.findByIdAndFarmUserId(id, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Plot not found or access denied")); // Should use specific
                                                                                             // exception
        return toResponse(plot);
    }

    @Transactional
    public PlotResponse updatePlotForCurrentFarmer(Integer id, PlotRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Plot plot = plotRepository.findByIdAndFarmUserId(id, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Plot not found or access denied"));

        plot.setPlotName(request.getPlotName());
        plot.setArea(request.getArea());
        plot.setSoilType(request.getSoilType());
        if (request.getStatus() != null) {
            plot.setStatus(request.getStatus().getCode());
        }

        Plot savedPlot = plotRepository.save(plot);
        return toResponse(savedPlot);
    }

    @Transactional
    public void deletePlotForCurrentFarmer(Integer id) {
        User currentUser = currentUserService.getCurrentUser();
        Plot plot = plotRepository.findByIdAndFarmUserId(id, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Plot not found or access denied"));

        // TODO: Check for dependencies (seasons, tasks etc) before delete
        plotRepository.delete(plot);
    }

    private PlotResponse toResponse(Plot plot) {
        return PlotResponse.builder()
                .id(plot.getId())
                .farmId(plot.getFarm().getId())
                .farmName(plot.getFarm().getName())
                .plotName(plot.getPlotName())
                .area(plot.getArea())
                .soilType(plot.getSoilType())
                .status(PlotStatus.fromCode(plot.getStatus()))
                .build();
    }
}
