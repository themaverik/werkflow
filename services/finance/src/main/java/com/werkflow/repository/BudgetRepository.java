package com.werkflow.repository;

import com.werkflow.entity.Budget;
import com.werkflow.entity.CapExCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Budget entity
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Find budgets by budget year
     */
    List<Budget> findByBudgetYear(Integer budgetYear);

    /**
     * Find budgets by category
     */
    List<Budget> findByCategory(CapExCategory category);

    /**
     * Find budgets by department name
     */
    List<Budget> findByDepartmentName(String departmentName);

    /**
     * Find budget by year, category, and department
     */
    Optional<Budget> findByBudgetYearAndCategoryAndDepartmentName(
        Integer budgetYear,
        CapExCategory category,
        String departmentName
    );

    /**
     * Find all budgets for a specific year and category
     */
    List<Budget> findByBudgetYearAndCategory(Integer budgetYear, CapExCategory category);

    /**
     * Find all active budgets
     */
    List<Budget> findByIsActive(Boolean isActive);

    /**
     * Find budgets with remaining amount
     */
    @Query("SELECT b FROM Budget b WHERE b.remainingAmount > 0 AND b.isActive = true")
    List<Budget> findBudgetsWithAvailableFunds();

    /**
     * Find budgets by year and department
     */
    List<Budget> findByBudgetYearAndDepartmentName(Integer budgetYear, String departmentName);

    /**
     * Count budgets by year
     */
    long countByBudgetYear(Integer budgetYear);

    /**
     * Count budgets by category
     */
    long countByCategory(CapExCategory category);

    /**
     * Find budgets where utilization exceeds threshold
     */
    @Query("SELECT b FROM Budget b WHERE " +
           "(b.utilizedAmount / b.allocatedAmount) >= :threshold AND b.isActive = true")
    List<Budget> findBudgetsAboveUtilizationThreshold(@Param("threshold") double threshold);
}
