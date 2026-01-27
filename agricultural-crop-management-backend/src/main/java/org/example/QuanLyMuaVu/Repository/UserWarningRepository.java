package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.UserWarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {
}
