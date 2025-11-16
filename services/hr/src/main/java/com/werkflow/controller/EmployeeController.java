package com.werkflow.controller;

import com.werkflow.dto.EmployeeRequest;
import com.werkflow.dto.EmployeeResponse;
import com.werkflow.entity.EmploymentStatus;
import com.werkflow.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Employee operations
 */
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management APIs")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @Operation(summary = "Get all employees", description = "Retrieve a list of all employees")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Retrieve an employee by their ID")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/code/{employeeCode}")
    @Operation(summary = "Get employee by code", description = "Retrieve an employee by their employee code")
    public ResponseEntity<EmployeeResponse> getEmployeeByCode(@PathVariable String employeeCode) {
        return ResponseEntity.ok(employeeService.getEmployeeByCode(employeeCode));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get employee by email", description = "Retrieve an employee by their email")
    public ResponseEntity<EmployeeResponse> getEmployeeByEmail(@PathVariable String email) {
        return ResponseEntity.ok(employeeService.getEmployeeByEmail(email));
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get employees by department", description = "Retrieve all employees in a department")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(employeeService.getEmployeesByDepartment(departmentId));
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Get employees by manager", description = "Retrieve all employees under a manager")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByManager(@PathVariable Long managerId) {
        return ResponseEntity.ok(employeeService.getEmployeesByManager(managerId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get employees by status", description = "Retrieve employees by employment status")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByStatus(@PathVariable EmploymentStatus status) {
        return ResponseEntity.ok(employeeService.getEmployeesByStatus(status));
    }

    @GetMapping("/search")
    @Operation(summary = "Search employees", description = "Search employees by name or email")
    public ResponseEntity<List<EmployeeResponse>> searchEmployees(@RequestParam String searchTerm) {
        return ResponseEntity.ok(employeeService.searchEmployees(searchTerm));
    }

    @PostMapping
    @Operation(summary = "Create employee", description = "Create a new employee")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Update an existing employee")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee", description = "Delete an employee")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
