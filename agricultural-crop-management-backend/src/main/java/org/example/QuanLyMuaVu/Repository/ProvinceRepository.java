package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {

    /**
     * Search provinces by name (case-insensitive).
     */
    List<Province> findByNameContainingIgnoreCase(String keyword);

    /**
     * Filter provinces by type ("thanh-pho" for city, "tinh" for province).
     */
    List<Province> findByType(String type);

    /**
     * Search provinces by name and filter by type.
     */
    List<Province> findByNameContainingIgnoreCaseAndType(String keyword, String type);
}
