package org.example.QuanLyMuaVu.Mapper;

import org.example.QuanLyMuaVu.DTO.Request.CropRequest;
import org.example.QuanLyMuaVu.DTO.Response.CropResponse;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CropMapper {
    public Crop toEntity(CropRequest request) {
        if (request == null) return null;
        return Crop.builder()
                .cropName(request.getCropName())
                .description(request.getDescription())
                .build();
    }

    public void update(Crop crop, CropRequest request) {
        if (crop == null || request == null) return;
        crop.setCropName(request.getCropName());
        crop.setDescription(request.getDescription());
    }

    public CropResponse toResponse(Crop crop) {
        if (crop == null) return null;
        return CropResponse.builder()
                .id(crop.getId())
                .cropName(crop.getCropName())
                .description(crop.getDescription())
                .build();
    }
}
