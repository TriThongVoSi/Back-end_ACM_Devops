package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {

    /**
     * Get all wards belonging to a specific province.
     */
    List<Ward> findByProvinceId(Integer provinceId);

    /**
     * Search wards by name within a specific province.
     */
    List<Ward> findByProvinceIdAndNameContainingIgnoreCase(Integer provinceId, String keyword);

    /**
     * Check if any wards exist for a given province.
     */
    boolean existsByProvinceId(Integer provinceId);

    /**
     * Delete all wards by province ID.
     */
    void deleteByProvinceId(Integer provinceId);
}
