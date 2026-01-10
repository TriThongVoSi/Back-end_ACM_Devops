package org.example.QuanLyMuaVu.Controller;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Entity.Province;
import org.example.QuanLyMuaVu.Entity.Ward;
import org.example.QuanLyMuaVu.Repository.ProvinceRepository;
import org.example.QuanLyMuaVu.Repository.WardRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final ProvinceRepository provinceRepository;
    private final WardRepository wardRepository;

    @GetMapping("/provinces")
    public ResponseEntity<List<Province>> getAllProvinces() {
        return ResponseEntity.ok(provinceRepository.findAll());
    }

    @GetMapping("/wards")
    public ResponseEntity<List<Ward>> getWardsByProvince(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(wardRepository.findByProvinceId(provinceId));
    }
}
