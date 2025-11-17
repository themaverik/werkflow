package com.werkflow.inventory.controller;

import com.werkflow.inventory.dto.AssetDefinitionRequest;
import com.werkflow.inventory.dto.AssetDefinitionResponse;
import com.werkflow.inventory.service.AssetDefinitionService;
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
@RequestMapping("/api/inventory/definitions")
@RequiredArgsConstructor
@Tag(name = "Asset Definitions", description = "Asset definition (catalog) management APIs")
public class AssetDefinitionController {

    private final AssetDefinitionService definitionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create asset definition", description = "Create a new asset definition (ADMIN, SUPER_ADMIN)")
    public ResponseEntity<AssetDefinitionResponse> createDefinition(@Valid @RequestBody AssetDefinitionRequest request) {
        AssetDefinitionResponse response = definitionService.createDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset definition by ID", description = "Retrieve asset definition details by ID")
    public ResponseEntity<AssetDefinitionResponse> getDefinitionById(@PathVariable Long id) {
        AssetDefinitionResponse response = definitionService.getDefinitionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all asset definitions", description = "Retrieve all asset definitions")
    public ResponseEntity<List<AssetDefinitionResponse>> getAllDefinitions() {
        List<AssetDefinitionResponse> response = definitionService.getAllDefinitions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get definitions by category", description = "Retrieve asset definitions by category")
    public ResponseEntity<List<AssetDefinitionResponse>> getDefinitionsByCategory(@PathVariable Long categoryId) {
        List<AssetDefinitionResponse> response = definitionService.getDefinitionsByCategory(categoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search asset definitions", description = "Search asset definitions by name, manufacturer, or model")
    public ResponseEntity<List<AssetDefinitionResponse>> searchDefinitions(@RequestParam String query) {
        List<AssetDefinitionResponse> response = definitionService.searchDefinitions(query);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update asset definition", description = "Update an existing asset definition (ADMIN, SUPER_ADMIN)")
    public ResponseEntity<AssetDefinitionResponse> updateDefinition(
        @PathVariable Long id,
        @Valid @RequestBody AssetDefinitionRequest request
    ) {
        AssetDefinitionResponse response = definitionService.updateDefinition(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete asset definition", description = "Delete an asset definition (SUPER_ADMIN only)")
    public ResponseEntity<Void> deleteDefinition(@PathVariable Long id) {
        definitionService.deleteDefinition(id);
        return ResponseEntity.noContent().build();
    }
}
