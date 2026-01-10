package org.example.QuanLyMuaVu.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.DTO.Request.CropRequest;
import org.example.QuanLyMuaVu.DTO.Response.CropResponse;
import org.example.QuanLyMuaVu.DTO.Response.VarietyResponse;
import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.Mapper.CropMapper;
import org.example.QuanLyMuaVu.Mapper.VarietyMapper;
import org.example.QuanLyMuaVu.Repository.CropRepository;
import org.example.QuanLyMuaVu.Repository.VarietyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CropService {
    CropRepository cropRepository;
    CropMapper cropMapper;
    VarietyRepository varietyRepository;
    VarietyMapper varietyMapper;

    public CropResponse create(CropRequest request) {
        if (cropRepository.existsByCropNameIgnoreCase(request.getCropName())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        Crop crop = cropMapper.toEntity(request);
        return cropMapper.toResponse(cropRepository.save(crop));
    }

    public List<CropResponse> getAll() {
        return cropRepository.findAll().stream().map(cropMapper::toResponse).toList();
    }

    public CropResponse getById(Integer id) {
        return cropRepository.findById(id)
                .map(cropMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
    }

    public CropResponse update(Integer id, CropRequest request) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));

        if (!crop.getCropName().equalsIgnoreCase(request.getCropName())
                && cropRepository.existsByCropNameIgnoreCase(request.getCropName())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        cropMapper.update(crop, request);
        return cropMapper.toResponse(cropRepository.save(crop));
    }

    public void delete(Integer id) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        cropRepository.delete(crop);
    }

    public List<VarietyResponse> getVarietiesByCropId(Integer cropId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        return varietyRepository.findAllByCrop(crop)
                .stream()
                .map(varietyMapper::toResponse)
                .toList();
    }
}
