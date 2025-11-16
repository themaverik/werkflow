package com.werkflow.workflow.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Flowable delegate for notifying HR about approved leave requests.
 * In a production system, this would integrate with email/notification service.
 */
@Slf4j
@Component("notifyHRDelegate")
@RequiredArgsConstructor
public class NotifyHRDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Executing NotifyHRDelegate for process instance: {}", execution.getProcessInstanceId());

        Long leaveId = (Long) execution.getVariable("leaveId");
        Long employeeId = (Long) execution.getVariable("employeeId");
        String employeeName = (String) execution.getVariable("employeeName");
        String leaveType = (String) execution.getVariable("leaveType");
        LocalDate startDate = (LocalDate) execution.getVariable("startDate");
        LocalDate endDate = (LocalDate) execution.getVariable("endDate");

        // In production, integrate with email service
        log.info("Notification sent to HR: Employee {} (ID: {}) has approved leave from {} to {}",
                employeeName, employeeId, startDate, endDate);

        // Simulate email notification to HR
        String subject = String.format("Approved Leave Request - %s", employeeName);
        String message = String.format(
                "Dear HR Team,\n\nPlease note that the following leave request has been approved:\n\n" +
                        "Employee: %s (ID: %d)\n" +
                        "Leave Type: %s\n" +
                        "Start Date: %s\n" +
                        "End Date: %s\n" +
                        "Leave Request ID: %d\n\n" +
                        "Please update payroll and scheduling systems accordingly.\n\n" +
                        "Best regards,\nWorkflow System",
                employeeName, employeeId, leaveType, startDate, endDate, leaveId
        );

        log.debug("HR Email notification: Subject='{}', Message='{}'", subject, message);

        execution.setVariable("hrNotified", true);
    }
}
