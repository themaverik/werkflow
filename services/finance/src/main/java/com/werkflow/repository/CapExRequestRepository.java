package com.werkflow.repository;

import com.werkflow.entity.CapExCategory;
import com.werkflow.entity.CapExRequest;
import com.werkflow.entity.CapExStatus;
import com.werkflow.entity.Priority;
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
 * Repository for CapEx Request entity
 */
@Repository
public interface CapExRequestRepository extends JpaRepository<CapExRequest, Long> {

    /**
     * Find a CapEx request by request number
     */
    Optional<CapExRequest> findByRequestNumber(String requestNumber);

    /**
     * Find all CapEx requests by status
     */
    List<CapExRequest> findByStatus(CapExStatus status);

    /**
     * Find all CapEx requests by category
     */
    List<CapExRequest> findByCategory(CapExCategory category);

    /**
     * Find all CapEx requests by priority
     */
    List<CapExRequest> findByPriority(Priority priority);

    /**
     * Find all CapEx requests by requested by user
     */
    List<CapExRequest> findByRequestedBy(String requestedBy);

    /**
     * Find all CapEx requests by department
     */
    List<CapExRequest> findByDepartmentName(String departmentName);

    /**
     * Find all CapEx requests by budget year
     */
    List<CapExRequest> findByBudgetYear(Integer budgetYear);

    /**
     * Find all CapEx requests by status with pagination
     */
    Page<CapExRequest> findByStatus(CapExStatus status, Pageable pageable);

    /**
     * Find all CapEx requests by category with pagination
     */
    Page<CapExRequest> findByCategory(CapExCategory category, Pageable pageable);

    /**
     * Find all active CapEx requests
     */
    List<CapExRequest> findByIsActive(Boolean isActive);

    /**
     * Find CapEx requests within a date range
     */
    @Query("SELECT c FROM CapExRequest c WHERE c.requestDate BETWEEN :startDate AND :endDate")
    List<CapExRequest> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find CapEx requests with amount greater than or equal to specified value
     */
    @Query("SELECT c FROM CapExRequest c WHERE c.amount >= :amount")
    List<CapExRequest> findByAmountGreaterThanOrEqual(@Param("amount") BigDecimal amount);

    /**
     * Find CapEx requests pending approval
     */
    @Query("SELECT c FROM CapExRequest c WHERE c.status = 'PENDING_APPROVAL'")
    List<CapExRequest> findPendingApprovals();

    /**
     * Count CapEx requests by status
     */
    long countByStatus(CapExStatus status);

    /**
     * Count CapEx requests by category
     */
    long countByCategory(CapExCategory category);

    /**
     * Search CapEx requests by title or description
     */
    @Query("SELECT c FROM CapExRequest c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<CapExRequest> searchByTitleOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);
}
