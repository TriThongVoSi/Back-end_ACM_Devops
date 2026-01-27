package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {
    Optional<UserPreferences> findByUser_Id(Long userId);
}
