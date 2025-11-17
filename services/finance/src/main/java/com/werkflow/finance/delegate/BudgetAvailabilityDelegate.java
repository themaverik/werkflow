package com.werkflow.finance.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Delegate for checking budget availability in CapEx approval workflow
 */
@Slf4j
@Component("budgetAvailabilityDelegate")
@RequiredArgsConstructor
public class BudgetAvailabilityDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing BudgetAvailabilityDelegate for process instance: {}",
                 execution.getProcessInstanceId());

        try {
            // Get request parameters from process variables
            Long departmentId = (Long) execution.getVariable("departmentId");
            BigDecimal requestedAmount = (BigDecimal) execution.getVariable("amount");
            String costCenter = (String) execution.getVariable("costCenter");
            Integer fiscalYear = (Integer) execution.getVariable("fiscalYear");

            log.info("Checking budget availability - Department: {}, Amount: {}, Cost Center: {}, Fiscal Year: {}",
                     departmentId, requestedAmount, costCenter, fiscalYear);

            // TODO: Implement actual budget check logic
            // 1. Fetch budget plan for department and fiscal year
            // 2. Check if cost center exists in budget
            // 3. Calculate available budget = allocated - (spent + committed)
            // 4. Verify requested amount <= available budget

            // For now, simulate budget check
            boolean budgetAvailable = checkBudgetAvailability(departmentId, requestedAmount, costCenter, fiscalYear);

            // Set result as process variable
            execution.setVariable("budgetAvailable", budgetAvailable);

            if (budgetAvailable) {
                log.info("Budget check PASSED - Sufficient budget available");
            } else {
                log.warn("Budget check FAILED - Insufficient budget");
                execution.setVariable("budgetShortfallReason", "Insufficient budget allocation for cost center");
            }

        } catch (Exception e) {
            log.error("Error during budget availability check", e);
            execution.setVariable("budgetAvailable", false);
            execution.setVariable("budgetShortfallReason", "Error during budget check: " + e.getMessage());
        }
    }

    private boolean checkBudgetAvailability(Long departmentId, BigDecimal requestedAmount,
                                           String costCenter, Integer fiscalYear) {
        // Placeholder implementation
        // In real implementation, this would:
        // 1. Query BudgetPlan and BudgetLineItem entities
        // 2. Calculate spent amount from Expense records
        // 3. Calculate committed amount from pending CapEx requests
        // 4. Return true if: allocated - (spent + committed) >= requestedAmount

        return requestedAmount.compareTo(BigDecimal.valueOf(1000000)) <= 0;
    }
}
