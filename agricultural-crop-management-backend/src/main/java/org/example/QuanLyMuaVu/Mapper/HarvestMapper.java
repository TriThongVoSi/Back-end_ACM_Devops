package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Request.HarvestRequest;
import org.example.QuanLyMuaVu.DTO.Response.HarvestResponse;
import org.example.QuanLyMuaVu.Entity.Harvest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Primary
public class HarvestMapper {
    public void update(Harvest harvest, HarvestRequest request) {
        if (harvest == null || request == null)
            return;
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setQuantity(request.getQuantity());
        harvest.setUnit(request.getUnit());
        harvest.setGrade(request.getGrade());
        harvest.setNote(request.getNote());
    }

    public HarvestResponse toResponse(Harvest harvest) {
        if (harvest == null)
            return null;
        BigDecimal revenue = null;
        if (harvest.getQuantity() != null && harvest.getUnit() != null) {
            revenue = harvest.getQuantity().multiply(harvest.getUnit());
        }
        return HarvestResponse.builder()
                .id(harvest.getId())
                .seasonId(harvest.getSeason() != null ? harvest.getSeason().getId() : null)
                .seasonName(harvest.getSeason() != null ? harvest.getSeason().getSeasonName() : null)
                .harvestDate(harvest.getHarvestDate())
                .quantity(harvest.getQuantity())
                .unit(harvest.getUnit())
                .grade(harvest.getGrade())
                .revenue(revenue)
                .note(harvest.getNote())
                .createdAt(harvest.getCreatedAt())
                .build();
    }
}
