package org.example.QuanLyMuaVu.Service.Admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.DTO.Response.FarmResponse;
import org.example.QuanLyMuaVu.Entity.Farm;
import org.example.QuanLyMuaVu.Repository.FarmRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Farm Query Service
 * Read-only queries for admin to view all farms across the system.
 * Uses existing FarmRepository and FarmResponse DTO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminFarmQueryService {

    private final FarmRepository farmRepository;

    /**
     * Get all farms with pagination (admin global view)
     * Uses LEFT JOIN FETCH to handle missing User references gracefully.
     */
    public Page<FarmResponse> getAllFarms(Pageable pageable) {
        log.info("Admin fetching all farms, page: {}", pageable.getPageNumber());

        Page<Farm> farms = farmRepository.findAllWithRelationships(pageable);

        return farms.map(this::toFarmResponse);
    }

    /**
     * Get all farms with optional keyword search
     * Uses LEFT JOIN FETCH to handle missing User references gracefully.
     */
    public Page<FarmResponse> searchFarms(String keyword, Pageable pageable) {
        log.info("Admin searching farms with keyword: {}", keyword);

        Page<Farm> farms;
        if (keyword != null && !keyword.isBlank()) {
            farms = farmRepository.findByNameContainingIgnoreCaseWithRelationships(keyword, pageable);
        } else {
            farms = farmRepository.findAllWithRelationships(pageable);
        }

        return farms.map(this::toFarmResponse);
    }

    /**
     * Get farm count by status
     */
    public long countActiveFarms() {
        return farmRepository.countByIsActiveTrue();
    }

    public long countInactiveFarms() {
        return farmRepository.countByIsActiveFalse();
    }

    /**
     * Map Farm entity to FarmResponse DTO
     * Matches frontend expectations with both farmName and name fields,
     * plus ownerUsername.
     */
    private FarmResponse toFarmResponse(Farm farm) {
        String ownerUsername = null;
        if (farm.getUser() != null) {
            ownerUsername = farm.getUser().getUsername();
        }

        return FarmResponse.builder()
                .id(farm.getId())
                .farmName(farm.getName())
                .name(farm.getName()) // Alias for frontend compatibility
                .ownerUsername(ownerUsername)
                .provinceId(farm.getProvince() != null ? farm.getProvince().getId() : null)
                .provinceName(farm.getProvince() != null ? farm.getProvince().getName() : null)
                .wardId(farm.getWard() != null ? farm.getWard().getId() : null)
                .wardName(farm.getWard() != null ? farm.getWard().getName() : null)
                .area(farm.getArea())
                .active(farm.getActive())
                .build();
    }
}
