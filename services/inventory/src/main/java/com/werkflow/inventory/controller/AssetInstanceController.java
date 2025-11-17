package com.werkflow.inventory.controller;

import com.werkflow.inventory.dto.AssetInstanceRequest;
import com.werkflow.inventory.dto.AssetInstanceResponse;
import com.werkflow.inventory.entity.AssetInstance;
import com.werkflow.inventory.service.AssetInstanceService;
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
@RequestMapping("/api/inventory/assets")
@RequiredArgsConstructor
@Tag(name = "Asset Instances", description = "Physical asset instance management APIs")
public class AssetInstanceController {

    private final AssetInstanceService instanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Create asset instance", description = "Create a new physical asset instance")
    public ResponseEntity<AssetInstanceResponse> createInstance(@Valid @RequestBody AssetInstanceRequest request) {
        AssetInstanceResponse response = instanceService.createInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset instance by ID", description = "Retrieve asset instance details by ID")
    public ResponseEntity<AssetInstanceResponse> getInstanceById(@PathVariable Long id) {
        AssetInstanceResponse response = instanceService.getInstanceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tag/{assetTag}")
    @Operation(summary = "Get asset instance by tag", description = "Retrieve asset instance details by asset tag")
    public ResponseEntity<AssetInstanceResponse> getInstanceByTag(@PathVariable String assetTag) {
        AssetInstanceResponse response = instanceService.getInstanceByTag(assetTag);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all asset instances", description = "Retrieve all asset instances")
    public ResponseEntity<List<AssetInstanceResponse>> getAllInstances() {
        List<AssetInstanceResponse> response = instanceService.getAllInstances();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/definition/{definitionId}")
    @Operation(summary = "Get instances by definition", description = "Retrieve asset instances by definition")
    public ResponseEntity<List<AssetInstanceResponse>> getInstancesByDefinition(@PathVariable Long definitionId) {
        List<AssetInstanceResponse> response = instanceService.getInstancesByDefinition(definitionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get instances by status", description = "Retrieve asset instances by status")
    public ResponseEntity<List<AssetInstanceResponse>> getInstancesByStatus(@PathVariable AssetInstance.AssetStatus status) {
        List<AssetInstanceResponse> response = instanceService.getInstancesByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search asset instances", description = "Search asset instances by tag, serial number, or name")
    public ResponseEntity<List<AssetInstanceResponse>> searchInstances(@RequestParam String query) {
        List<AssetInstanceResponse> response = instanceService.searchInstances(query);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Update asset instance", description = "Update an existing asset instance")
    public ResponseEntity<AssetInstanceResponse> updateInstance(
        @PathVariable Long id,
        @Valid @RequestBody AssetInstanceRequest request
    ) {
        AssetInstanceResponse response = instanceService.updateInstance(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete asset instance", description = "Delete an asset instance (SUPER_ADMIN only)")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }
}
