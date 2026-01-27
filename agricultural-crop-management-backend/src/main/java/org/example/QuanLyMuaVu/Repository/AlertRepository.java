package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.Alert;
import org.example.QuanLyMuaVu.Enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    Optional<Alert> findFirstByFarmIdAndTypeAndCreatedAtBetween(
            Integer farmId,
            AlertType type,
            LocalDateTime start,
            LocalDateTime end);
}
