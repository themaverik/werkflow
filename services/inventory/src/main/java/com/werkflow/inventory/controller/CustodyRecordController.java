package com.werkflow.inventory.controller;

import com.werkflow.inventory.dto.CustodyRecordRequest;
import com.werkflow.inventory.dto.CustodyRecordResponse;
import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.service.CustodyRecordService;
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
@RequestMapping("/api/inventory/custody")
@RequiredArgsConstructor
@Tag(name = "Custody Records", description = "Asset custody tracking APIs")
public class CustodyRecordController {

    private final CustodyRecordService custodyRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Create custody record", description = "Create a new custody record")
    public ResponseEntity<CustodyRecordResponse> createCustodyRecord(@Valid @RequestBody CustodyRecordRequest request) {
        CustodyRecordResponse response = custodyRecordService.createCustodyRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get custody record by ID", description = "Retrieve custody record details by ID")
    public ResponseEntity<CustodyRecordResponse> getCustodyRecordById(@PathVariable Long id) {
        CustodyRecordResponse response = custodyRecordService.getCustodyRecordById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/asset/{assetId}/current")
    @Operation(summary = "Get current custody for asset", description = "Retrieve current custody record for an asset")
    public ResponseEntity<CustodyRecordResponse> getCurrentCustodyForAsset(@PathVariable Long assetId) {
        return custodyRecordService.getCurrentCustodyForAsset(assetId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/asset/{assetId}/history")
    @Operation(summary = "Get custody history for asset", description = "Retrieve complete custody history for an asset")
    public ResponseEntity<List<CustodyRecordResponse>> getCustodyHistoryForAsset(@PathVariable Long assetId) {
        List<CustodyRecordResponse> response = custodyRecordService.getCustodyHistoryForAsset(assetId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{deptId}")
    @Operation(summary = "Get current custody by department", description = "Retrieve all current custody records for a department")
    public ResponseEntity<List<CustodyRecordResponse>> getCurrentCustodyByDepartment(@PathVariable Long deptId) {
        List<CustodyRecordResponse> response = custodyRecordService.getCurrentCustodyByDepartment(deptId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get current custody by user", description = "Retrieve all current custody records for a user")
    public ResponseEntity<List<CustodyRecordResponse>> getCurrentCustodyByUser(@PathVariable Long userId) {
        List<CustodyRecordResponse> response = custodyRecordService.getCurrentCustodyByUser(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Transfer custody", description = "Transfer custody of an asset to another department/user")
    public ResponseEntity<CustodyRecordResponse> transferCustody(
        @RequestParam Long assetInstanceId,
        @RequestParam Long toDeptId,
        @RequestParam(required = false) Long toUserId,
        @RequestParam Long assignedByUserId,
        @RequestParam(required = false) String notes
    ) {
        CustodyRecordResponse response = custodyRecordService.transferCustody(
            assetInstanceId, toDeptId, toUserId, assignedByUserId, notes
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Close custody record", description = "Close an active custody record")
    public ResponseEntity<Void> closeCustody(
        @PathVariable Long id,
        @RequestParam AssetInstance.AssetCondition returnCondition,
        @RequestParam(required = false) String notes
    ) {
        custodyRecordService.closeCustody(id, returnCondition, notes);
        return ResponseEntity.noContent().build();
    }
}
