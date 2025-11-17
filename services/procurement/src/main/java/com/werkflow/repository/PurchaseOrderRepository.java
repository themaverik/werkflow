package com.werkflow.repository;

import com.werkflow.entity.PurchaseOrder;
import com.werkflow.entity.PurchaseOrderStatus;
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
 * Repository for Purchase Order entity
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Find a purchase order by PO number
     */
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    /**
     * Find purchase orders by status
     */
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    /**
     * Find purchase orders by status with pagination
     */
    Page<PurchaseOrder> findByStatus(PurchaseOrderStatus status, Pageable pageable);

    /**
     * Find purchase orders by vendor ID
     */
    List<PurchaseOrder> findByVendorId(Long vendorId);

    /**
     * Find purchase orders by purchase request ID
     */
    List<PurchaseOrder> findByPurchaseRequestId(Long purchaseRequestId);

    /**
     * Find purchase order by purchase request ID (expecting single result)
     */
    Optional<PurchaseOrder> findFirstByPurchaseRequestId(Long purchaseRequestId);

    /**
     * Find all active purchase orders
     */
    List<PurchaseOrder> findByIsActive(Boolean isActive);

    /**
     * Find purchase orders within a date range
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.poDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find purchase orders with amount greater than or equal to specified value
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.poAmount >= :amount")
    List<PurchaseOrder> findByAmountGreaterThanOrEqual(@Param("amount") BigDecimal amount);

    /**
     * Find overdue purchase orders
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.deliveryDate < CURRENT_DATE AND p.status IN ('DRAFT', 'SUBMITTED', 'SENT', 'ACKNOWLEDGED', 'IN_FULFILLMENT', 'IN_DELIVERY')")
    List<PurchaseOrder> findOverdueOrders();

    /**
     * Count purchase orders by status
     */
    long countByStatus(PurchaseOrderStatus status);

    /**
     * Search purchase orders by PO number or vendor name
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE " +
           "LOWER(p.poNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.vendorName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PurchaseOrder> searchByPoNumberOrVendorName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find purchase orders created by specific user
     */
    List<PurchaseOrder> findByCreatedByUser(String createdByUser);

    /**
     * Find purchase orders pending acknowledgment
     */
    @Query("SELECT p FROM PurchaseOrder p WHERE p.status = 'SENT' AND p.acknowledgedDate IS NULL")
    List<PurchaseOrder> findPendingAcknowledgment();
}
