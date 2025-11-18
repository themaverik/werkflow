package com.werkflow.repository;

import com.werkflow.entity.Priority;
import com.werkflow.entity.PurchaseRequest;
import com.werkflow.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Purchase Request entity
 */
@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    /**
     * Find a purchase request by request number
     */
    Optional<PurchaseRequest> findByRequestNumber(String requestNumber);

    /**
     * Find all purchase requests by status
     */
    List<PurchaseRequest> findByStatus(RequestStatus status);

    /**
     * Find all purchase requests by priority
     */
    List<PurchaseRequest> findByPriority(Priority priority);

    /**
     * Find all purchase requests by requested by user
     */
    List<PurchaseRequest> findByRequestedBy(String requestedBy);

    /**
     * Find all purchase requests by department
     */
    List<PurchaseRequest> findByDepartmentName(String departmentName);

    /**
     * Find all purchase requests by status with pagination
     */
    Page<PurchaseRequest> findByStatus(RequestStatus status, Pageable pageable);

    /**
     * Find all active purchase requests
     */
    List<PurchaseRequest> findByIsActive(Boolean isActive);

    /**
     * Find purchase requests within a date range
     */
    @Query("SELECT p FROM PurchaseRequest p WHERE p.requestDate BETWEEN :startDate AND :endDate")
    List<PurchaseRequest> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find purchase requests with amount greater than or equal to specified value
     */
    @Query("SELECT p FROM PurchaseRequest p WHERE p.totalAmount >= :amount")
    List<PurchaseRequest> findByAmountGreaterThanOrEqual(@Param("amount") BigDecimal amount);

    /**
     * Find purchase requests pending approval
     */
    @Query("SELECT p FROM PurchaseRequest p WHERE p.status = 'PENDING_APPROVAL'")
    List<PurchaseRequest> findPendingApprovals();

    /**
     * Count purchase requests by status
     */
    long countByStatus(RequestStatus status);

    /**
     * Search purchase requests by title or description
     */
    @Query("SELECT p FROM PurchaseRequest p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PurchaseRequest> searchByTitleOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find purchase requests by preferred vendor
     */
    List<PurchaseRequest> findByPreferredVendorId(Long vendorId);

    /**
     * Find purchase requests by selected vendor
     */
    List<PurchaseRequest> findBySelectedVendorId(Long vendorId);
}
