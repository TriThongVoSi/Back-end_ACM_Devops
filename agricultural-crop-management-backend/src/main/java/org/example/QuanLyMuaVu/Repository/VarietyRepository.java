package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Crop;
import org.example.QuanLyMuaVu.Entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VarietyRepository extends JpaRepository<Variety, Integer> {

    List<Variety> findAllByCrop(Crop crop);
}
