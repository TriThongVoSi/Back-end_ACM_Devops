package org.example.QuanLyMuaVu.Repository;

import org.example.QuanLyMuaVu.Entity.StockLocation;
import org.example.QuanLyMuaVu.Entity.StockMovement;
import org.example.QuanLyMuaVu.Entity.SupplyLot;
import org.example.QuanLyMuaVu.Entity.Warehouse;
import org.example.QuanLyMuaVu.Enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

        @Query("""
                        select coalesce(sum(
                            case when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.IN then m.quantity
                                 when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.OUT then -m.quantity
                                 else m.quantity end
                        ), 0)
                        from StockMovement m
                        where m.supplyLot = :lot
                          and m.warehouse = :warehouse
                          and (:location is null or m.location = :location)
                        """)
        BigDecimal calculateOnHandQuantity(
                        @Param("lot") SupplyLot lot,
                        @Param("warehouse") Warehouse warehouse,
                        @Param("location") StockLocation location);

        /**
         * Find distinct supply lot IDs with movements at a warehouse/location
         */
        @Query("""
                        select distinct m.supplyLot.id
                        from StockMovement m
                        where m.warehouse = :warehouse
                          and (:location is null or m.location = :location)
                        """)
        List<Integer> findDistinctSupplyLotIdsByWarehouse(
                        @Param("warehouse") Warehouse warehouse,
                        @Param("location") StockLocation location);

        /**
         * Paginated movement history with filters
         */
        @Query("""
                        select m from StockMovement m
                        where m.warehouse = :warehouse
                          and (:type is null or m.movementType = :type)
                          and (:from is null or m.movementDate >= :from)
                          and (:to is null or m.movementDate <= :to)
                        order by m.movementDate desc
                        """)
        Page<StockMovement> findByWarehouseWithFilters(
                        @Param("warehouse") Warehouse warehouse,
                        @Param("type") StockMovementType type,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        Pageable pageable);

        /**
         * Find all movements for a warehouse (simpler version)
         */
        Page<StockMovement> findByWarehouseOrderByMovementDateDesc(Warehouse warehouse, Pageable pageable);

        @Query("""
                        select f.id as farmId,
                               f.name as farmName,
                               sl.id as lotId,
                               si.name as itemName,
                               sl.expiryDate as expiryDate,
                               sum(case when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.IN then m.quantity
                                        when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.OUT then -m.quantity
                                        else m.quantity end) as onHand
                        from StockMovement m
                        join m.warehouse w
                        join w.farm f
                        join m.supplyLot sl
                        join sl.supplyItem si
                        where (:farmId is null or f.id = :farmId)
                        group by f.id, f.name, sl.id, si.name, sl.expiryDate
                        having sum(case when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.IN then m.quantity
                                        when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.OUT then -m.quantity
                                        else m.quantity end) > 0
                        """)
        List<InventoryLotOnHandProjection> findOnHandLotsByFarm(@Param("farmId") Integer farmId);

        @Query("""
                        select f.id as farmId,
                               f.name as farmName,
                               sl.id as lotId,
                               si.id as itemId,
                               si.name as itemName,
                               sl.batchCode as lotCode,
                               sl.expiryDate as expiryDate,
                               si.unit as unit,
                               sum(case when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.IN then m.quantity
                                        when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.OUT then -m.quantity
                                        else m.quantity end) as onHand
                        from StockMovement m
                        join m.warehouse w
                        join w.farm f
                        join m.supplyLot sl
                        join sl.supplyItem si
                        where (:farmId is null or f.id = :farmId)
                          and (:q is null or lower(si.name) like lower(concat('%', :q, '%'))
                               or lower(sl.batchCode) like lower(concat('%', :q, '%')))
                        group by f.id, f.name, sl.id, si.id, si.name, sl.batchCode, sl.expiryDate, si.unit
                        having sum(case when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.IN then m.quantity
                                        when m.movementType = org.example.QuanLyMuaVu.Enums.StockMovementType.OUT then -m.quantity
                                        else m.quantity end) > 0
                        """)
        List<InventoryLotRiskProjection> findOnHandLotsWithDetails(
                        @Param("farmId") Integer farmId,
                        @Param("q") String q);

        List<StockMovement> findBySupplyLot_IdOrderByMovementDateDesc(Integer supplyLotId);

        interface InventoryLotOnHandProjection {
                Integer getFarmId();

                String getFarmName();

                Integer getLotId();

                String getItemName();

                java.time.LocalDate getExpiryDate();

                BigDecimal getOnHand();
        }

        interface InventoryLotRiskProjection {
                Integer getFarmId();

                String getFarmName();

                Integer getLotId();

                Integer getItemId();

                String getItemName();

                String getLotCode();

                java.time.LocalDate getExpiryDate();

                String getUnit();

                BigDecimal getOnHand();
        }
}
