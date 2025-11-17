package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.MaintenanceRecord;
import com.werkflow.inventory.entity.MaintenanceRecord.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByAssetInstanceId(Long assetInstanceId);

    List<MaintenanceRecord> findByMaintenanceType(MaintenanceType type);

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.assetInstance.id = :assetId " +
           "AND mr.maintenanceType = :type ORDER BY mr.completedDate DESC")
    List<MaintenanceRecord> findByAssetIdAndType(
        @Param("assetId") Long assetInstanceId,
        @Param("type") MaintenanceType type
    );

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.scheduledDate BETWEEN :startDate AND :endDate")
    List<MaintenanceRecord> findScheduledBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.nextMaintenanceDate BETWEEN :startDate AND :endDate")
    List<MaintenanceRecord> findUpcomingMaintenance(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.scheduledDate IS NOT NULL " +
           "AND mr.completedDate IS NULL AND mr.scheduledDate < :date")
    List<MaintenanceRecord> findOverdueMaintenanceBefore(@Param("date") LocalDate date);

    @Query("SELECT mr FROM MaintenanceRecord mr WHERE mr.assetInstance.id = :assetId " +
           "ORDER BY mr.completedDate DESC LIMIT 1")
    MaintenanceRecord findLatestByAssetId(@Param("assetId") Long assetInstanceId);

    @Query("SELECT SUM(mr.cost) FROM MaintenanceRecord mr WHERE mr.assetInstance.id = :assetId")
    java.math.BigDecimal calculateTotalMaintenanceCost(@Param("assetId") Long assetInstanceId);
}
