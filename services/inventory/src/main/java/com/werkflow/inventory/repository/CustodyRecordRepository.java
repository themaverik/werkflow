package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.CustodyRecord;
import com.werkflow.inventory.entity.CustodyRecord.CustodyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustodyRecordRepository extends JpaRepository<CustodyRecord, Long> {

    List<CustodyRecord> findByAssetInstanceId(Long assetInstanceId);

    @Query("SELECT cr FROM CustodyRecord cr WHERE cr.assetInstance.id = :assetId AND cr.endDate IS NULL")
    Optional<CustodyRecord> findCurrentCustodyByAssetId(@Param("assetId") Long assetInstanceId);

    List<CustodyRecord> findByCustodianDeptId(Long deptId);

    List<CustodyRecord> findByCustodianUserId(Long userId);

    @Query("SELECT cr FROM CustodyRecord cr WHERE cr.custodianDeptId = :deptId AND cr.endDate IS NULL")
    List<CustodyRecord> findCurrentCustodyByDepartment(@Param("deptId") Long deptId);

    @Query("SELECT cr FROM CustodyRecord cr WHERE cr.custodianUserId = :userId AND cr.endDate IS NULL")
    List<CustodyRecord> findCurrentCustodyByUser(@Param("userId") Long userId);

    @Query("SELECT cr FROM CustodyRecord cr WHERE cr.custodyType = :type AND cr.endDate IS NULL")
    List<CustodyRecord> findActiveCustodyByType(@Param("type") CustodyType type);

    @Query("SELECT COUNT(cr) FROM CustodyRecord cr WHERE cr.custodianDeptId = :deptId AND cr.endDate IS NULL")
    Long countCurrentAssetsInDepartment(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(cr) FROM CustodyRecord cr WHERE cr.custodianUserId = :userId AND cr.endDate IS NULL")
    Long countCurrentAssetsWithUser(@Param("userId") Long userId);
}
