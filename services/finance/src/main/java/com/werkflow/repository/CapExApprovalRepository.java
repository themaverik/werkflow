package com.werkflow.repository;

import com.werkflow.entity.ApprovalLevel;
import com.werkflow.entity.CapExApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CapEx Approval entity
 */
@Repository
public interface CapExApprovalRepository extends JpaRepository<CapExApproval, Long> {

    /**
     * Find all approvals for a specific CapEx request
     */
    List<CapExApproval> findByCapexRequestIdOrderByApprovalOrder(Long capexRequestId);

    /**
     * Find approvals for a specific CapEx request and approval level
     */
    Optional<CapExApproval> findByCapexRequestIdAndApprovalLevel(Long capexRequestId, ApprovalLevel approvalLevel);

    /**
     * Find all approvals for a specific approver
     */
    List<CapExApproval> findByApprover(String approver);

    /**
     * Find all approvals with specific status
     */
    List<CapExApproval> findByApprovalStatus(String approvalStatus);

    /**
     * Find pending approvals for a specific approver
     */
    @Query("SELECT ca FROM CapExApproval ca WHERE ca.approver = :approver AND ca.approvalStatus = 'PENDING'")
    List<CapExApproval> findPendingApprovalsForApprover(@Param("approver") String approver);

    /**
     * Find all approvals for a CapEx request with specific status
     */
    List<CapExApproval> findByCapexRequestIdAndApprovalStatus(Long capexRequestId, String approvalStatus);

    /**
     * Count approvals by status
     */
    long countByApprovalStatus(String approvalStatus);

    /**
     * Find all active approvals
     */
    List<CapExApproval> findByIsActive(Boolean isActive);

    /**
     * Check if there are pending approvals for a request
     */
    @Query("SELECT CASE WHEN COUNT(ca) > 0 THEN true ELSE false END FROM CapExApproval ca " +
           "WHERE ca.capexRequestId = :capexRequestId AND ca.approvalStatus = 'PENDING'")
    boolean hasPendingApprovals(@Param("capexRequestId") Long capexRequestId);
}
