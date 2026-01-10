package org.example.QuanLyMuaVu.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.example.QuanLyMuaVu.Entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    void deleteByCode(String code);
    Optional<Role> findByCode(String code);
    List<Role> findByCodeIn(Collection<String> codes);
}
