package org.example.QuanLyMuaVu.Repository;

import jakarta.persistence.LockModeType;
import org.example.QuanLyMuaVu.Entity.InventoryBalance;
import org.example.QuanLyMuaVu.Entity.StockLocation;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    /**
     * Find inventory balance with pessimistic write lock for upsert operations.
     * Used for IN movements and positive ADJUST movements.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
              AND ((:location IS NULL AND ib.location IS NULL) OR ib.location = :location)
            """)
    Optional<InventoryBalance> findByLotAndWarehouseAndLocationWithLock(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse,
            @Param("location") StockLocation location);

    /**
     * Find inventory balance without lock (for read operations).
     */
    @Query("""
            SELECT ib FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
              AND ((:location IS NULL AND ib.location IS NULL) OR ib.location = :location)
            """)
    Optional<InventoryBalance> findByLotAndWarehouseAndLocation(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse,
            @Param("location") StockLocation location);

    /**
     * Atomic deduct operation with stock check.
     * Returns 1 if successful, 0 if insufficient stock.
     * Used for OUT movements and negative ADJUST movements.
     */
    @Modifying
    @Query("""
            UPDATE InventoryBalance ib
            SET ib.quantity = ib.quantity - :qty
            WHERE ib.id = :id AND ib.quantity >= :qty
            """)
    int atomicDeduct(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * Get current quantity for a supply lot at a warehouse/location.
     */
    @Query("""
            SELECT COALESCE(ib.quantity, 0)
            FROM InventoryBalance ib
            WHERE ib.supplyLot = :lot
              AND ib.warehouse = :warehouse
              AND ((:location IS NULL AND ib.location IS NULL) OR ib.location = :location)
            """)
    BigDecimal getCurrentQuantity(
            @Param("lot") SupplyLot lot,
            @Param("warehouse") Warehouse warehouse,
            @Param("location") StockLocation location);
}
