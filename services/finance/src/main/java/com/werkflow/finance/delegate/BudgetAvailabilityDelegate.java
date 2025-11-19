package com.werkflow.finance.delegate;

import com.werkflow.finance.dto.BudgetCheckRequest;
import com.werkflow.finance.dto.BudgetCheckResponse;
import com.werkflow.finance.service.BudgetCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Delegate for checking budget availability in CapEx approval workflow
 * This delegate is for LOCAL budget checks within the Finance service.
 * For cross-service budget checks, use RestServiceDelegate in BPMN.
 */
@Slf4j
@Component("budgetAvailabilityDelegate")
@RequiredArgsConstructor
public class BudgetAvailabilityDelegate implements JavaDelegate {

    private final BudgetCheckService budgetCheckService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing BudgetAvailabilityDelegate for process instance: {}",
                 execution.getProcessInstanceId());

        try {
            Long departmentId = (Long) execution.getVariable("departmentId");
            BigDecimal requestedAmount = (BigDecimal) execution.getVariable("amount");
            String costCenter = (String) execution.getVariable("costCenter");
            Integer fiscalYear = (Integer) execution.getVariable("fiscalYear");

            log.info("Checking budget availability - Department: {}, Amount: {}, Cost Center: {}, Fiscal Year: {}",
                     departmentId, requestedAmount, costCenter, fiscalYear);

            BudgetCheckRequest request = BudgetCheckRequest.builder()
                    .departmentId(departmentId)
                    .amount(requestedAmount)
                    .costCenter(costCenter)
                    .fiscalYear(fiscalYear)
                    .build();

            BudgetCheckResponse response = budgetCheckService.checkBudgetAvailability(request);

            execution.setVariable("budgetAvailable", response.isAvailable());
            execution.setVariable("budgetAvailableAmount", response.getAvailableAmount());
            execution.setVariable("budgetAllocatedAmount", response.getAllocatedAmount());
            execution.setVariable("budgetUtilizedAmount", response.getUtilizedAmount());

            if (response.isAvailable()) {
                log.info("Budget check PASSED - {}", response.getReason());
            } else {
                log.warn("Budget check FAILED - {}", response.getReason());
                execution.setVariable("budgetShortfallReason", response.getReason());
            }

        } catch (Exception e) {
            log.error("Error during budget availability check", e);
            execution.setVariable("budgetAvailable", false);
            execution.setVariable("budgetShortfallReason", "Error during budget check: " + e.getMessage());
        }
    }
}
