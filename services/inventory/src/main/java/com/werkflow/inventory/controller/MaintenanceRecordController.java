package com.werkflow.inventory.controller;

import com.werkflow.inventory.dto.MaintenanceRecordRequest;
import com.werkflow.inventory.dto.MaintenanceRecordResponse;
import com.werkflow.inventory.service.MaintenanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance Records", description = "Asset maintenance tracking APIs")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER', 'MAINTENANCE_STAFF')")
    @Operation(summary = "Create maintenance record", description = "Create a new maintenance record")
    public ResponseEntity<MaintenanceRecordResponse> createMaintenanceRecord(@Valid @RequestBody MaintenanceRecordRequest request) {
        MaintenanceRecordResponse response = maintenanceRecordService.createMaintenanceRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance record by ID", description = "Retrieve maintenance record details by ID")
    public ResponseEntity<MaintenanceRecordResponse> getMaintenanceRecordById(@PathVariable Long id) {
        MaintenanceRecordResponse response = maintenanceRecordService.getMaintenanceRecordById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/asset/{assetId}")
    @Operation(summary = "Get maintenance records by asset", description = "Retrieve all maintenance records for an asset")
    public ResponseEntity<List<MaintenanceRecordResponse>> getMaintenanceRecordsByAsset(@PathVariable Long assetId) {
        List<MaintenanceRecordResponse> response = maintenanceRecordService.getMaintenanceRecordsByAsset(assetId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scheduled")
    @Operation(summary = "Get scheduled maintenance", description = "Retrieve scheduled maintenance within a date range")
    public ResponseEntity<List<MaintenanceRecordResponse>> getScheduledMaintenance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<MaintenanceRecordResponse> response = maintenanceRecordService.getScheduledMaintenance(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming maintenance", description = "Retrieve upcoming maintenance within a date range")
    public ResponseEntity<List<MaintenanceRecordResponse>> getUpcomingMaintenance(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<MaintenanceRecordResponse> response = maintenanceRecordService.getUpcomingMaintenance(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue maintenance", description = "Retrieve overdue maintenance records")
    public ResponseEntity<List<MaintenanceRecordResponse>> getOverdueMaintenance() {
        List<MaintenanceRecordResponse> response = maintenanceRecordService.getOverdueMaintenance();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER', 'MAINTENANCE_STAFF')")
    @Operation(summary = "Complete maintenance record", description = "Mark a maintenance record as completed")
    public ResponseEntity<MaintenanceRecordResponse> completeMaintenanceRecord(
        @PathVariable Long id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDate,
        @RequestParam(required = false) String notes
    ) {
        MaintenanceRecordResponse response = maintenanceRecordService.completeMaintenanceRecord(id, completedDate, notes);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Update maintenance record", description = "Update an existing maintenance record")
    public ResponseEntity<MaintenanceRecordResponse> updateMaintenanceRecord(
        @PathVariable Long id,
        @Valid @RequestBody MaintenanceRecordRequest request
    ) {
        MaintenanceRecordResponse response = maintenanceRecordService.updateMaintenanceRecord(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete maintenance record", description = "Delete a maintenance record (SUPER_ADMIN only)")
    public ResponseEntity<Void> deleteMaintenanceRecord(@PathVariable Long id) {
        maintenanceRecordService.deleteMaintenanceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
