package com.werkflow.service;

import com.werkflow.entity.Budget;
import com.werkflow.entity.CapExCategory;
import com.werkflow.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Budget operations
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;

    /**
     * Create a new budget
     */
    public Budget createBudget(Budget budget) {
        log.info("Creating new budget for year: {}, category: {}", budget.getBudgetYear(), budget.getCategory());

        budget.calculateRemainingAmount();
        Budget saved = budgetRepository.save(budget);

        log.info("Budget created successfully with id: {}", saved.getId());
        return saved;
    }

    /**
     * Get budget by ID
     */
    @Transactional(readOnly = true)
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
    }

    /**
     * Get all budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    /**
     * Get budgets by year
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByYear(Integer budgetYear) {
        return budgetRepository.findByBudgetYear(budgetYear);
    }

    /**
     * Get budgets by category
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByCategory(CapExCategory category) {
        return budgetRepository.findByCategory(category);
    }

    /**
     * Get budgets by department
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByDepartment(String departmentName) {
        return budgetRepository.findByDepartmentName(departmentName);
    }

    /**
     * Get budget by year, category, and department
     */
    @Transactional(readOnly = true)
    public Budget getBudgetByYearCategoryDept(Integer budgetYear, CapExCategory category, String departmentName) {
        return budgetRepository.findByBudgetYearAndCategoryAndDepartmentName(budgetYear, category, departmentName)
            .orElseThrow(() -> new EntityNotFoundException(
                "Budget not found for year: " + budgetYear + ", category: " + category + ", department: " + departmentName
            ));
    }

    /**
     * Get budgets with available funds
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsWithAvailableFunds() {
        return budgetRepository.findBudgetsWithAvailableFunds();
    }

    /**
     * Update budget
     */
    public Budget updateBudget(Long id, Budget budgetDetails) {
        log.info("Updating budget with id: {}", id);

        Budget budget = getBudgetById(id);

        budget.setAllocatedAmount(budgetDetails.getAllocatedAmount());
        budget.setUtilizedAmount(budgetDetails.getUtilizedAmount());
        budget.setRemarks(budgetDetails.getRemarks());
        budget.calculateRemainingAmount();

        Budget updated = budgetRepository.save(budget);
        log.info("Budget updated successfully with id: {}", id);

        return updated;
    }

    /**
     * Add to budget utilization
     */
    public Budget addBudgetUtilization(Long id, BigDecimal amount) {
        log.info("Adding budget utilization of {} to budget with id: {}", amount, id);

        Budget budget = getBudgetById(id);

        if (budget.getUtilizedAmount() == null) {
            budget.setUtilizedAmount(BigDecimal.ZERO);
        }

        BigDecimal newUtilized = budget.getUtilizedAmount().add(amount);

        if (newUtilized.compareTo(budget.getAllocatedAmount()) > 0) {
            throw new IllegalArgumentException("Budget utilization would exceed allocated amount");
        }

        budget.setUtilizedAmount(newUtilized);
        budget.calculateRemainingAmount();

        Budget updated = budgetRepository.save(budget);
        log.info("Budget utilization updated successfully");

        return updated;
    }

    /**
     * Get budgets above utilization threshold
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsAboveUtilizationThreshold(double threshold) {
        log.info("Fetching budgets above utilization threshold: {}", threshold);
        return budgetRepository.findBudgetsAboveUtilizationThreshold(threshold);
    }

    /**
     * Deactivate budget
     */
    public Budget deactivateBudget(Long id) {
        log.info("Deactivating budget with id: {}", id);

        Budget budget = getBudgetById(id);
        budget.setIsActive(false);

        Budget updated = budgetRepository.save(budget);
        log.info("Budget deactivated successfully");

        return updated;
    }

    /**
     * Get active budgets
     */
    @Transactional(readOnly = true)
    public List<Budget> getActiveBudgets() {
        return budgetRepository.findByIsActive(true);
    }

    /**
     * Calculate total allocated budget for a year
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalAllocatedForYear(Integer budgetYear) {
        return getBudgetsByYear(budgetYear).stream()
            .map(Budget::getAllocatedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total utilized budget for a year
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUtilizedForYear(Integer budgetYear) {
        return getBudgetsByYear(budgetYear).stream()
            .map(Budget::getUtilizedAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate total remaining budget for a year
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRemainingForYear(Integer budgetYear) {
        return getBudgetsByYear(budgetYear).stream()
            .map(Budget::getRemainingAmount)
            .filter(amount -> amount != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
