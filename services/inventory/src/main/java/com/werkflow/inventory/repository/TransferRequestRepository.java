package com.werkflow.inventory.repository;

import com.werkflow.inventory.entity.TransferRequest;
import com.werkflow.inventory.entity.TransferRequest.TransferStatus;
import com.werkflow.inventory.entity.TransferRequest.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    List<TransferRequest> findByAssetInstanceId(Long assetInstanceId);

    List<TransferRequest> findByFromDeptId(Long deptId);

    List<TransferRequest> findByToDeptId(Long deptId);

    List<TransferRequest> findByInitiatedByUserId(Long userId);

    List<TransferRequest> findByStatus(TransferStatus status);

    Optional<TransferRequest> findByProcessInstanceId(String processInstanceId);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.status = :status AND tr.transferType = :type")
    List<TransferRequest> findByStatusAndType(
        @Param("status") TransferStatus status,
        @Param("type") TransferType type
    );

    @Query("SELECT tr FROM TransferRequest tr WHERE " +
           "(tr.fromDeptId = :deptId OR tr.toDeptId = :deptId) AND tr.status = :status")
    List<TransferRequest> findByDepartmentAndStatus(
        @Param("deptId") Long deptId,
        @Param("status") TransferStatus status
    );

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.fromDeptId = :deptId OR tr.toDeptId = :deptId")
    List<TransferRequest> findAllByDepartment(@Param("deptId") Long deptId);

    @Query("SELECT tr FROM TransferRequest tr WHERE tr.status = 'PENDING' " +
           "AND tr.assetInstance.purchaseCost >= :threshold")
    List<TransferRequest> findPendingHighValueTransfers(@Param("threshold") java.math.BigDecimal threshold);

    @Query("SELECT COUNT(tr) FROM TransferRequest tr WHERE tr.status = 'PENDING' AND tr.toDeptId = :deptId")
    Long countPendingRequestsForDepartment(@Param("deptId") Long deptId);
}
