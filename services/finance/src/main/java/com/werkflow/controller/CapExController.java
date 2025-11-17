package com.werkflow.controller;

import com.werkflow.dto.CapExRequestDto;
import com.werkflow.dto.CapExResponseDto;
import com.werkflow.entity.CapExCategory;
import com.werkflow.entity.CapExStatus;
import com.werkflow.entity.Priority;
import com.werkflow.service.CapExService;
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
 * REST Controller for CapEx Request operations
 */
@RestController
@RequestMapping("/capex")
@RequiredArgsConstructor
@Tag(name = "CapEx Requests", description = "CapEx request management APIs")
public class CapExController {

    private final CapExService capexService;

    @PostMapping
    @Operation(summary = "Create CapEx request", description = "Create a new CapEx request")
    public ResponseEntity<CapExResponseDto> createCapExRequest(
            @Valid @RequestBody CapExRequestDto requestDto,
            Authentication authentication) {
        String requestedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(capexService.createCapExRequest(requestDto, requestedBy));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get CapEx request by ID", description = "Retrieve a CapEx request by its ID")
    public ResponseEntity<CapExResponseDto> getCapExRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(capexService.getCapExRequestById(id));
    }

    @GetMapping("/number/{requestNumber}")
    @Operation(summary = "Get CapEx request by number", description = "Retrieve a CapEx request by its request number")
    public ResponseEntity<CapExResponseDto> getCapExRequestByNumber(@PathVariable String requestNumber) {
        return ResponseEntity.ok(capexService.getCapExRequestByNumber(requestNumber));
    }

    @GetMapping
    @Operation(summary = "Get all CapEx requests", description = "Retrieve all CapEx requests with pagination")
    public ResponseEntity<Page<CapExResponseDto>> getAllCapExRequests(Pageable pageable) {
        return ResponseEntity.ok(capexService.getCapExRequests(pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get CapEx requests by status", description = "Retrieve CapEx requests by status")
    public ResponseEntity<List<CapExResponseDto>> getCapExRequestsByStatus(@PathVariable CapExStatus status) {
        return ResponseEntity.ok(capexService.getCapExRequestsByStatus(status));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get CapEx requests by category", description = "Retrieve CapEx requests by category")
    public ResponseEntity<List<CapExResponseDto>> getCapExRequestsByCategory(@PathVariable CapExCategory category) {
        return ResponseEntity.ok(capexService.getCapExRequestsByCategory(category));
    }

    @GetMapping("/priority/{priority}")
    @Operation(summary = "Get CapEx requests by priority", description = "Retrieve CapEx requests by priority")
    public ResponseEntity<List<CapExResponseDto>> getCapExRequestsByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(capexService.getCapExRequestsByPriority(priority));
    }

    @GetMapping("/requested-by/{requestedBy}")
    @Operation(summary = "Get CapEx requests by requested user", description = "Retrieve CapEx requests created by a specific user")
    public ResponseEntity<List<CapExResponseDto>> getCapExRequestsByRequestedBy(@PathVariable String requestedBy) {
        return ResponseEntity.ok(capexService.getCapExRequestsByRequestedBy(requestedBy));
    }

    @GetMapping("/department/{departmentName}")
    @Operation(summary = "Get CapEx requests by department", description = "Retrieve CapEx requests for a specific department")
    public ResponseEntity<List<CapExResponseDto>> getCapExRequestsByDepartment(@PathVariable String departmentName) {
        return ResponseEntity.ok(capexService.getCapExRequestsByDepartment(departmentName));
    }

    @GetMapping("/budget-year/{budgetYear}")
    @Operation(summary = "Get CapEx requests by budget year", description = "Retrieve CapEx requests for a specific budget year")
    public ResponseEntity<List<CapExResponseDto>> getCapExRequestsByBudgetYear(@PathVariable Integer budgetYear) {
        return ResponseEntity.ok(capexService.getCapExRequestsByBudgetYear(budgetYear));
    }

    @GetMapping("/search")
    @Operation(summary = "Search CapEx requests", description = "Search CapEx requests by title or description")
    public ResponseEntity<Page<CapExResponseDto>> searchCapExRequests(
            @RequestParam String searchTerm,
            Pageable pageable) {
        return ResponseEntity.ok(capexService.searchCapExRequests(searchTerm, pageable));
    }

    @GetMapping("/pending-approvals")
    @Operation(summary = "Get pending approvals", description = "Retrieve all CapEx requests pending approval")
    public ResponseEntity<List<CapExResponseDto>> getPendingApprovals() {
        return ResponseEntity.ok(capexService.getPendingApprovals());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update CapEx request", description = "Update an existing CapEx request")
    public ResponseEntity<CapExResponseDto> updateCapExRequest(
            @PathVariable Long id,
            @Valid @RequestBody CapExRequestDto requestDto) {
        return ResponseEntity.ok(capexService.updateCapExRequest(id, requestDto));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve CapEx request", description = "Approve a CapEx request")
    public ResponseEntity<CapExResponseDto> approveCapExRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            Authentication authentication) {
        String approvedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(capexService.approveCapExRequest(id, approvedBy, remarks));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject CapEx request", description = "Reject a CapEx request")
    public ResponseEntity<CapExResponseDto> rejectCapExRequest(
            @PathVariable Long id,
            @RequestParam String rejectionReason,
            Authentication authentication) {
        String rejectedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(capexService.rejectCapExRequest(id, rejectedBy, rejectionReason));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update CapEx request status", description = "Update the status of a CapEx request")
    public ResponseEntity<CapExResponseDto> updateCapExRequestStatus(
            @PathVariable Long id,
            @RequestParam CapExStatus status) {
        return ResponseEntity.ok(capexService.updateCapExRequestStatus(id, status));
    }

    @GetMapping("/summary/statistics")
    @Operation(summary = "Get CapEx statistics", description = "Get summary statistics for CapEx requests")
    public ResponseEntity<Map<String, Object>> getCapExStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        List<CapExResponseDto> allRequests = capexService.getAllCapExRequests();
        statistics.put("total_requests", allRequests.size());
        statistics.put("pending_requests",
            allRequests.stream().filter(r -> r.getStatus() == CapExStatus.PENDING_APPROVAL).count());
        statistics.put("approved_requests",
            allRequests.stream().filter(r -> r.getStatus() == CapExStatus.APPROVED).count());
        statistics.put("rejected_requests",
            allRequests.stream().filter(r -> r.getStatus() == CapExStatus.REJECTED).count());

        return ResponseEntity.ok(statistics);
    }
}
