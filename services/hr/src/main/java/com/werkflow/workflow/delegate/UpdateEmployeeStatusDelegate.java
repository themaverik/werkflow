package com.werkflow.workflow.delegate;

import com.werkflow.entity.Employee;
import com.werkflow.entity.EmploymentStatus;
import com.werkflow.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Flowable delegate for updating employee status after successful onboarding.
 * Updates employee status to ACTIVE in the database.
 */
@Slf4j
@Component("updateEmployeeStatusDelegate")
@RequiredArgsConstructor
public class UpdateEmployeeStatusDelegate implements JavaDelegate {

    private final EmployeeRepository employeeRepository;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing UpdateEmployeeStatusDelegate for process instance: {}", execution.getProcessInstanceId());

        Long employeeId = (Long) execution.getVariable("employeeId");

        if (employeeId == null) {
            log.error("Employee ID is null in process instance: {}", execution.getProcessInstanceId());
            throw new IllegalArgumentException("Employee ID cannot be null");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with ID: " + employeeId));

        // Update employee status to ACTIVE after successful onboarding
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employeeRepository.save(employee);

        log.info("Employee {} (ID: {}) status updated to ACTIVE after onboarding completion",
                employee.getFirstName() + " " + employee.getLastName(), employeeId);

        execution.setVariable("employeeStatusUpdated", true);
        execution.setVariable("finalEmployeeStatus", "ACTIVE");
    }
}
