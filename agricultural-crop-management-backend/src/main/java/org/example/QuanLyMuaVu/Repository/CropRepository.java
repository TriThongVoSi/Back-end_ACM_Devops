package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropRepository extends JpaRepository<Crop, Integer> {
    List<Crop> findByCropNameContainingIgnoreCase(String name);

    boolean existsByCropNameIgnoreCase(String cropName);
}
