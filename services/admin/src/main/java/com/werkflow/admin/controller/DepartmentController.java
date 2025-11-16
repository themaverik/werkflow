package com.werkflow.admin.controller;

import com.werkflow.admin.dto.DepartmentRequest;
import com.werkflow.admin.dto.DepartmentResponse;
import com.werkflow.admin.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department management APIs")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create department", description = "Create a new department (ADMIN, SUPER_ADMIN)")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve department details by ID")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get departments by organization", description = "Retrieve all departments for an organization")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByOrganization(@PathVariable Long organizationId) {
        List<DepartmentResponse> response = departmentService.getDepartmentsByOrganization(organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organization/{organizationId}/top-level")
    @Operation(summary = "Get top-level departments", description = "Retrieve top-level departments (without parent) for an organization")
    public ResponseEntity<List<DepartmentResponse>> getTopLevelDepartments(@PathVariable Long organizationId) {
        List<DepartmentResponse> response = departmentService.getTopLevelDepartments(organizationId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update department", description = "Update an existing department (ADMIN, SUPER_ADMIN)")
    public ResponseEntity<DepartmentResponse> updateDepartment(
        @PathVariable Long id,
        @Valid @RequestBody DepartmentRequest request
    ) {
        DepartmentResponse response = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete department", description = "Delete a department (SUPER_ADMIN only)")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
