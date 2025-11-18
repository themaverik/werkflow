package com.werkflow.controller;

import com.werkflow.dto.PurchaseRequestDto;
import com.werkflow.dto.PurchaseRequestResponseDto;
import com.werkflow.entity.Priority;
import com.werkflow.entity.RequestStatus;
import com.werkflow.service.ProcurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Purchase Request operations
 */
@RestController
@RequestMapping("/procurement/requests")
@RequiredArgsConstructor
@Tag(name = "Purchase Requests", description = "Purchase request management APIs")
public class ProcurementController {

    private final ProcurementService procurementService;

    @PostMapping
    @Operation(summary = "Create purchase request", description = "Create a new purchase request")
    public ResponseEntity<PurchaseRequestResponseDto> createPurchaseRequest(
            @Valid @RequestBody PurchaseRequestDto requestDto,
            Authentication authentication) {
        String requestedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(procurementService.createPurchaseRequest(requestDto, requestedBy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase request by ID", description = "Retrieve a purchase request by its ID")
    public ResponseEntity<PurchaseRequestResponseDto> getPurchaseRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(procurementService.getPurchaseRequestById(id));
    }

    @GetMapping("/number/{requestNumber}")
    @Operation(summary = "Get purchase request by number", description = "Retrieve a purchase request by its request number")
    public ResponseEntity<PurchaseRequestResponseDto> getPurchaseRequestByNumber(@PathVariable String requestNumber) {
        return ResponseEntity.ok(procurementService.getPurchaseRequestByNumber(requestNumber));
    }

    @GetMapping
    @Operation(summary = "Get all purchase requests", description = "Retrieve all purchase requests with pagination")
    public ResponseEntity<Page<PurchaseRequestResponseDto>> getAllPurchaseRequests(Pageable pageable) {
        return ResponseEntity.ok(procurementService.getPurchaseRequests(pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get purchase requests by status", description = "Retrieve purchase requests by status")
    public ResponseEntity<List<PurchaseRequestResponseDto>> getPurchaseRequestsByStatus(@PathVariable RequestStatus status) {
        return ResponseEntity.ok(procurementService.getPurchaseRequestsByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    @Operation(summary = "Get purchase requests by priority", description = "Retrieve purchase requests by priority")
    public ResponseEntity<List<PurchaseRequestResponseDto>> getPurchaseRequestsByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(procurementService.getPurchaseRequestsByPriority(priority));
    }

    @GetMapping("/requested-by/{requestedBy}")
    @Operation(summary = "Get purchase requests by requested user", description = "Retrieve purchase requests created by a specific user")
    public ResponseEntity<List<PurchaseRequestResponseDto>> getPurchaseRequestsByRequestedBy(@PathVariable String requestedBy) {
        return ResponseEntity.ok(procurementService.getPurchaseRequestsByRequestedBy(requestedBy));
    }

    @GetMapping("/department/{departmentName}")
    @Operation(summary = "Get purchase requests by department", description = "Retrieve purchase requests for a specific department")
    public ResponseEntity<List<PurchaseRequestResponseDto>> getPurchaseRequestsByDepartment(@PathVariable String departmentName) {
        return ResponseEntity.ok(procurementService.getPurchaseRequestsByDepartment(departmentName));
    }

    @GetMapping("/search")
    @Operation(summary = "Search purchase requests", description = "Search purchase requests by title or description")
    public ResponseEntity<Page<PurchaseRequestResponseDto>> searchPurchaseRequests(
            @RequestParam String searchTerm,
            Pageable pageable) {
        return ResponseEntity.ok(procurementService.searchPurchaseRequests(searchTerm, pageable));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get pending approvals", description = "Retrieve all purchase requests pending approval")
    public ResponseEntity<List<PurchaseRequestResponseDto>> getPendingApprovals() {
        return ResponseEntity.ok(procurementService.getPendingApprovals());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update purchase request", description = "Update an existing purchase request")
    public ResponseEntity<PurchaseRequestResponseDto> updatePurchaseRequest(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequestDto requestDto) {
        return ResponseEntity.ok(procurementService.updatePurchaseRequest(id, requestDto));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve purchase request", description = "Approve a purchase request")
    public ResponseEntity<PurchaseRequestResponseDto> approvePurchaseRequest(
            @PathVariable Long id,
            Authentication authentication) {
        String approvedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(procurementService.approvePurchaseRequest(id, approvedBy));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject purchase request", description = "Reject a purchase request")
    public ResponseEntity<PurchaseRequestResponseDto> rejectPurchaseRequest(
            @PathVariable Long id,
            @RequestParam String rejectionReason,
            Authentication authentication) {
        String rejectedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(procurementService.rejectPurchaseRequest(id, rejectedBy, rejectionReason));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update purchase request status", description = "Update the status of a purchase request")
    public ResponseEntity<PurchaseRequestResponseDto> updatePurchaseRequestStatus(
            @PathVariable Long id,
            @RequestParam RequestStatus status) {
        return ResponseEntity.ok(procurementService.updatePurchaseRequestStatus(id, status));
    }

    @GetMapping("/summary/statistics")
    @Operation(summary = "Get purchase request statistics", description = "Get summary statistics for purchase requests")
    public ResponseEntity<Map<String, Object>> getPurchaseRequestStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        List<PurchaseRequestResponseDto> allRequests = procurementService.getAllPurchaseRequests();
        statistics.put("total_requests", allRequests.size());
        statistics.put("pending_requests",
            allRequests.stream().filter(r -> r.getStatus() == RequestStatus.PENDING_APPROVAL).count());
        statistics.put("approved_requests",
            allRequests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count());
        statistics.put("rejected_requests",
            allRequests.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count());

        return ResponseEntity.ok(statistics);
    }
}
