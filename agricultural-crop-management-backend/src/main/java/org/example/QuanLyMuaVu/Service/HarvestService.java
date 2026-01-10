package org.example.QuanLyMuaVu.Service;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.DTO.Request.HarvestRequest;
import org.example.QuanLyMuaVu.DTO.Response.HarvestResponse;
import org.example.QuanLyMuaVu.Entity.Harvest;
import org.example.QuanLyMuaVu.Mapper.HarvestMapper;
import org.example.QuanLyMuaVu.Repository.HarvestRepository;
import org.example.QuanLyMuaVu.Repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HarvestService {

    private final HarvestRepository harvestRepository;
    private final SeasonRepository seasonRepository;
    private final HarvestMapper harvestMapper;

    public HarvestResponse create(HarvestRequest request) {
        var season = seasonRepository.findById(request.getSeasonId()).orElseThrow();
        Harvest harvest = Harvest.builder().season(season).build();
        harvestMapper.update(harvest, request);
        harvest = harvestRepository.save(harvest);
        return harvestMapper.toResponse(harvest);
    }

    public List<HarvestResponse> getAll() {
        return harvestRepository.findAll().stream().map(harvestMapper::toResponse).toList();
    }

    public HarvestResponse update(Integer id, HarvestRequest request) {
        Harvest harvest = harvestRepository.findById(id).orElseThrow();
        harvestMapper.update(harvest, request);
        return harvestMapper.toResponse(harvestRepository.save(harvest));
    }

    public void delete(Integer id) {
        harvestRepository.deleteById(id);
    }
}
