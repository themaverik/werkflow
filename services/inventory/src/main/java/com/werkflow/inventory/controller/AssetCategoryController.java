package com.werkflow.inventory.controller;

import com.werkflow.inventory.dto.AssetCategoryRequest;
import com.werkflow.inventory.dto.AssetCategoryResponse;
import com.werkflow.inventory.service.AssetCategoryService;
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
@RequestMapping("/api/inventory/categories")
@RequiredArgsConstructor
@Tag(name = "Asset Categories", description = "Asset category management APIs")
public class AssetCategoryController {

    private final AssetCategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create asset category", description = "Create a new asset category (ADMIN, SUPER_ADMIN)")
    public ResponseEntity<AssetCategoryResponse> createCategory(@Valid @RequestBody AssetCategoryRequest request) {
        AssetCategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset category by ID", description = "Retrieve asset category details by ID")
    public ResponseEntity<AssetCategoryResponse> getCategoryById(@PathVariable Long id) {
        AssetCategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all asset categories", description = "Retrieve all asset categories")
    public ResponseEntity<List<AssetCategoryResponse>> getAllCategories() {
        List<AssetCategoryResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-level")
    @Operation(summary = "Get top-level categories", description = "Retrieve top-level categories (without parent)")
    public ResponseEntity<List<AssetCategoryResponse>> getTopLevelCategories() {
        List<AssetCategoryResponse> response = categoryService.getTopLevelCategories();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{deptId}")
    @Operation(summary = "Get categories by department", description = "Retrieve categories by primary custodian department")
    public ResponseEntity<List<AssetCategoryResponse>> getCategoriesByDepartment(@PathVariable Long deptId) {
        List<AssetCategoryResponse> response = categoryService.getCategoriesByDepartment(deptId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update asset category", description = "Update an existing asset category (ADMIN, SUPER_ADMIN)")
    public ResponseEntity<AssetCategoryResponse> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody AssetCategoryRequest request
    ) {
        AssetCategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete asset category", description = "Delete an asset category (SUPER_ADMIN only)")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
