package com.werkflow.inventory.controller;

import com.werkflow.inventory.dto.TransferRequestRequest;
import com.werkflow.inventory.dto.TransferRequestResponse;
import com.werkflow.inventory.service.TransferRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer Requests", description = "Asset transfer request management APIs")
public class TransferRequestController {

    private final TransferRequestService transferRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Create transfer request", description = "Create a new asset transfer request")
    public ResponseEntity<TransferRequestResponse> createTransferRequest(@Valid @RequestBody TransferRequestRequest request) {
        TransferRequestResponse response = transferRequestService.createTransferRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer request by ID", description = "Retrieve transfer request details by ID")
    public ResponseEntity<TransferRequestResponse> getTransferRequestById(@PathVariable Long id) {
        TransferRequestResponse response = transferRequestService.getTransferRequestById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/asset/{assetId}")
    @Operation(summary = "Get transfer requests by asset", description = "Retrieve all transfer requests for an asset")
    public ResponseEntity<List<TransferRequestResponse>> getTransferRequestsByAsset(@PathVariable Long assetId) {
        List<TransferRequestResponse> response = transferRequestService.getTransferRequestsByAsset(assetId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{deptId}")
    @Operation(summary = "Get transfer requests by department", description = "Retrieve all transfer requests involving a department")
    public ResponseEntity<List<TransferRequestResponse>> getTransferRequestsByDepartment(@PathVariable Long deptId) {
        List<TransferRequestResponse> response = transferRequestService.getTransferRequestsByDepartment(deptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending transfer requests", description = "Retrieve all pending transfer requests")
    public ResponseEntity<List<TransferRequestResponse>> getPendingTransferRequests() {
        List<TransferRequestResponse> response = transferRequestService.getPendingTransferRequests();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending/high-value")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FINANCE_MANAGER')")
    @Operation(summary = "Get high-value pending transfers", description = "Retrieve pending transfers for high-value assets")
    public ResponseEntity<List<TransferRequestResponse>> getHighValuePendingTransfers() {
        List<TransferRequestResponse> response = transferRequestService.getHighValuePendingTransfers();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "Approve transfer request", description = "Approve a pending transfer request")
    public ResponseEntity<TransferRequestResponse> approveTransferRequest(
        @PathVariable Long id,
        @RequestParam Long approvedByUserId
    ) {
        TransferRequestResponse response = transferRequestService.approveTransferRequest(id, approvedByUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "Reject transfer request", description = "Reject a pending transfer request")
    public ResponseEntity<TransferRequestResponse> rejectTransferRequest(
        @PathVariable Long id,
        @RequestParam String rejectionReason
    ) {
        TransferRequestResponse response = transferRequestService.rejectTransferRequest(id, rejectionReason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Complete transfer request", description = "Complete an approved transfer request")
    public ResponseEntity<TransferRequestResponse> completeTransferRequest(
        @PathVariable Long id,
        @RequestParam Long completedByUserId
    ) {
        TransferRequestResponse response = transferRequestService.completeTransferRequest(id, completedByUserId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Cancel transfer request", description = "Cancel a transfer request")
    public ResponseEntity<Void> cancelTransferRequest(@PathVariable Long id) {
        transferRequestService.cancelTransferRequest(id);
        return ResponseEntity.noContent().build();
    }
}
