package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.FieldLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FieldLogRepository extends JpaRepository<FieldLog, Integer> {

    List<FieldLog> findAllBySeason_Id(Integer seasonId);

    List<FieldLog> findAllBySeason_IdAndLogDateBetween(Integer seasonId, LocalDate from, LocalDate to);

    boolean existsBySeason_Id(Integer seasonId);
}
