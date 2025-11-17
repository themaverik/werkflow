package com.werkflow.controller;

import com.werkflow.entity.Budget;
import com.werkflow.entity.CapExCategory;
import com.werkflow.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Budget operations
 */
@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Budget management APIs")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "Create budget", description = "Create a new budget allocation")
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody Budget budget) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(budgetService.createBudget(budget));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get budget by ID", description = "Retrieve a budget by its ID")
    public ResponseEntity<Budget> getBudgetById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @GetMapping
    @Operation(summary = "Get all budgets", description = "Retrieve all budgets")
    public ResponseEntity<List<Budget>> getAllBudgets() {
        return ResponseEntity.ok(budgetService.getAllBudgets());
    }

    @GetMapping("/year/{budgetYear}")
    @Operation(summary = "Get budgets by year", description = "Retrieve budgets for a specific fiscal year")
    public ResponseEntity<List<Budget>> getBudgetsByYear(@PathVariable Integer budgetYear) {
        return ResponseEntity.ok(budgetService.getBudgetsByYear(budgetYear));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get budgets by category", description = "Retrieve budgets for a specific category")
    public ResponseEntity<List<Budget>> getBudgetsByCategory(@PathVariable CapExCategory category) {
        return ResponseEntity.ok(budgetService.getBudgetsByCategory(category));
    }

    @GetMapping("/department/{departmentName}")
    @Operation(summary = "Get budgets by department", description = "Retrieve budgets for a specific department")
    public ResponseEntity<List<Budget>> getBudgetsByDepartment(@PathVariable String departmentName) {
        return ResponseEntity.ok(budgetService.getBudgetsByDepartment(departmentName));
    }

    @GetMapping("/search")
    @Operation(summary = "Get budget by year, category, and department", description = "Retrieve specific budget allocation")
    public ResponseEntity<Budget> getBudgetByYearCategoryDept(
            @RequestParam Integer budgetYear,
            @RequestParam CapExCategory category,
            @RequestParam String departmentName) {
        return ResponseEntity.ok(budgetService.getBudgetByYearCategoryDept(budgetYear, category, departmentName));
    }

    @GetMapping("/available-funds")
    @Operation(summary = "Get budgets with available funds", description = "Retrieve all budgets with remaining balance")
    public ResponseEntity<List<Budget>> getBudgetsWithAvailableFunds() {
        return ResponseEntity.ok(budgetService.getBudgetsWithAvailableFunds());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active budgets", description = "Retrieve all active budgets")
    public ResponseEntity<List<Budget>> getActiveBudgets() {
        return ResponseEntity.ok(budgetService.getActiveBudgets());
    }

    @GetMapping("/utilization-threshold/{threshold}")
    @Operation(summary = "Get budgets above utilization threshold", description = "Retrieve budgets exceeding specified utilization percentage")
    public ResponseEntity<List<Budget>> getBudgetsAboveThreshold(@PathVariable double threshold) {
        return ResponseEntity.ok(budgetService.getBudgetsAboveUtilizationThreshold(threshold));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update budget", description = "Update an existing budget")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody Budget budgetDetails) {
        return ResponseEntity.ok(budgetService.updateBudget(id, budgetDetails));
    }

    @PutMapping("/{id}/add-utilization")
    @Operation(summary = "Add budget utilization", description = "Record a budget utilization amount")
    public ResponseEntity<Budget> addBudgetUtilization(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(budgetService.addBudgetUtilization(id, amount));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate budget", description = "Deactivate a budget")
    public ResponseEntity<Budget> deactivateBudget(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.deactivateBudget(id));
    }

    @GetMapping("/summary/year/{budgetYear}")
    @Operation(summary = "Get budget summary for year", description = "Get summary statistics for budgets in a specific year")
    public ResponseEntity<Map<String, Object>> getBudgetSummaryForYear(@PathVariable Integer budgetYear) {
        Map<String, Object> summary = new HashMap<>();

        BigDecimal totalAllocated = budgetService.getTotalAllocatedForYear(budgetYear);
        BigDecimal totalUtilized = budgetService.getTotalUtilizedForYear(budgetYear);
        BigDecimal totalRemaining = budgetService.getTotalRemainingForYear(budgetYear);

        summary.put("budget_year", budgetYear);
        summary.put("total_allocated", totalAllocated);
        summary.put("total_utilized", totalUtilized);
        summary.put("total_remaining", totalRemaining);
        summary.put("utilization_percentage", calculateUtilizationPercentage(totalAllocated, totalUtilized));
        summary.put("budget_count", budgetService.getBudgetsByYear(budgetYear).size());

        return ResponseEntity.ok(summary);
    }

    private double calculateUtilizationPercentage(BigDecimal allocated, BigDecimal utilized) {
        if (allocated == null || allocated.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return (utilized.doubleValue() / allocated.doubleValue()) * 100;
    }
}
