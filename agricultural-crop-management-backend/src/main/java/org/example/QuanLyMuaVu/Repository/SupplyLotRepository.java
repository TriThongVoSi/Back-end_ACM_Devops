package org.example.QuanLyMuaVu.Repository;

import java.util.List;

import org.example.QuanLyMuaVu.Entity.Supplier;
import org.example.QuanLyMuaVu.Entity.SupplyItem;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplyLotRepository extends JpaRepository<SupplyLot, Integer> {

        List<SupplyLot> findAllBySupplyItem(SupplyItem item);

        List<SupplyLot> findAllBySupplier(Supplier supplier);

        boolean existsBySupplierId(Integer supplierId);

        @Query("""
                        SELECT l FROM SupplyLot l
                        LEFT JOIN FETCH l.supplyItem
                        LEFT JOIN FETCH l.supplier
                        WHERE (:itemId IS NULL OR l.supplyItem.id = :itemId)
                          AND (:supplierId IS NULL OR l.supplier.id = :supplierId)
                          AND (:status IS NULL OR l.status = :status)
                          AND (:q IS NULL OR LOWER(l.batchCode) LIKE LOWER(CONCAT('%', :q, '%')))
                        ORDER BY l.id DESC
                        """)
        Page<SupplyLot> searchLots(
                        @Param("itemId") Integer itemId,
                        @Param("supplierId") Integer supplierId,
                        @Param("status") String status,
                        @Param("q") String q,
                        Pageable pageable);

        @Query("""
                        SELECT l FROM SupplyLot l
                        LEFT JOIN FETCH l.supplyItem
                        LEFT JOIN FETCH l.supplier
                        WHERE l.id = :lotId
                        """)
        java.util.Optional<SupplyLot> findByIdWithDetails(@Param("lotId") Integer lotId);
}
