package com.werkflow.procurement.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Delegate for checking budget availability via Finance Service API
 */
@Slf4j
@Component("financeBudgetCheckDelegate")
@RequiredArgsConstructor
public class FinanceBudgetCheckDelegate implements JavaDelegate {

    private final RestTemplate restTemplate;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing FinanceBudgetCheckDelegate for PR - Process Instance: {}",
                 execution.getProcessInstanceId());

        try {
            Long departmentId = (Long) execution.getVariable("departmentId");
            BigDecimal totalAmount = (BigDecimal) execution.getVariable("totalAmount");
            String costCenter = (String) execution.getVariable("costCenter");

            log.info("Checking budget via Finance Service - Department: {}, Amount: {}",
                     departmentId, totalAmount);

            // Call Finance Service API to check budget
            boolean budgetAvailable = callFinanceServiceForBudgetCheck(departmentId, totalAmount, costCenter);

            execution.setVariable("budgetAvailable", budgetAvailable);

            if (budgetAvailable) {
                log.info("Budget check via Finance Service PASSED");
            } else {
                log.warn("Budget check via Finance Service FAILED");
            }

        } catch (Exception e) {
            log.error("Error calling Finance Service for budget check", e);
            execution.setVariable("budgetAvailable", false);
        }
    }

    private boolean callFinanceServiceForBudgetCheck(Long departmentId, BigDecimal amount, String costCenter) {
        try {
            // TODO: Replace with actual Finance Service URL from configuration
            String financeServiceUrl = "http://localhost:8085/api/budget/check";

            Map<String, Object> request = new HashMap<>();
            request.put("departmentId", departmentId);
            request.put("amount", amount);
            request.put("costCenter", costCenter);

            // Mock response for now
            // Map<String, Object> response = restTemplate.postForObject(financeServiceUrl, request, Map.class);
            // return (Boolean) response.get("available");

            // Placeholder: return true for amounts under 100K
            return amount.compareTo(BigDecimal.valueOf(100000)) <= 0;

        } catch (Exception e) {
            log.error("Error calling Finance Service API", e);
            return false;
        }
    }
}
